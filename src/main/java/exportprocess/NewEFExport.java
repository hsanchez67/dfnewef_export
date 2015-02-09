package exportprocess;

import client.RestClient;
import com.demandforce.core.Constants;
import com.demandforce.core.Customer;
import com.demandforce.db.DBUtils;
import com.demandforce.model.Business;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.jdbc.Statement;
import model.AppendJobDetail;
import model.Client;
import model.ClientJob;
import model.ClientJobDetail;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by hsanchez on 1/29/2015.
 */
public class NewEFExport {
    static Logger log = Logger.getLogger(NewEFExport.class.getName());

    private static final String QUERY_BUSINESS_EAPPEND_GROUP = " group by b.ID limit 1";
    private static final String QUERY_BUSINESS_EAPPEND_DAY = " and b.ID in (select id from Business where id % 28 = to_days(curdate()-1) % 28) ";
    private static final String QUERY_BUSINESS_EAPPEND_BID = " and b.ID = ? ";
    private static final String QUERY_BUSINESS_EAPPEND_SELECT = "select  b.* from BusinessOptions as bo, NodeGroup as ng, Business as b "
            + "where b.ID = bo.BusinessID and b.NodeGroup = ng.Value and ng.Name = 'NodeGroup' and "
            + "bo.OptionType = ? and bo.OptionValue = ? and b.IsTest != 1  and b.Country='US' "
            + " and not exists(select 1 from BusinessOptions bo52 where bo52.businessid = b.ID and bo52.optiontype =52 and bo52.optionvalue =1)";
    private static final String QUERY_BUSINESS_EAPPEND = QUERY_BUSINESS_EAPPEND_SELECT + QUERY_BUSINESS_EAPPEND_DAY + QUERY_BUSINESS_EAPPEND_GROUP;
    private static final String QUERY_BUSINESS_EAPPEND_CUSTOM = QUERY_BUSINESS_EAPPEND_SELECT + QUERY_BUSINESS_EAPPEND_BID + QUERY_BUSINESS_EAPPEND_GROUP;
    private static final String QUERY_BUSINESS_EAPPEND_BY_ID = QUERY_BUSINESS_EAPPEND_SELECT + QUERY_BUSINESS_EAPPEND_BID + QUERY_BUSINESS_EAPPEND_GROUP;
    private static final String QUERY_AJ_ID = "select AppendJobId from AppendJob as aj where aj.BusinessID = ? and  em.Period = ?";

    protected static final String _clientURl = "http://localhost:8080/dfbluebox-webservice/v1/client";
    protected static final String _clientJobURL = "http://localhost:8080/dfbluebox-webservice/v1/clientJob";

    public static void exportNode(String businessID, String ng, Connection connect) throws Exception {
        log.info(" BusinessID = " + businessID + " Node = "+ng);
        businessID = StringUtils.isBlank(businessID) ? "All" : businessID;

        if (connect == null || connect.isClosed()) {
            throw new RuntimeException("invalid connection");
        }
        List<Business> allBusiness = new ArrayList<Business>();
        if (!businessID.equalsIgnoreCase("All")) {
            Business b = getBusiness(businessID, connect);
            if (b != null) {
                allBusiness.add(b);
            }
        } else {
            allBusiness = getAllExportedBusiness(connect, businessID);
        }
        for (Business b : allBusiness) {
           exportForOneBusiness(b, businessID, connect);
        }
    }

    private static String currentTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new java.util.Date());
    }

    private static List<AppendJobDetail> exportForOneBusiness(Business tempBus, String businessID, Connection connect) throws Exception {
        int busID = 0;
        if (!businessID.equals("All")) busID = Integer.parseInt(businessID);
        int appendJobID;
        ClientJob clientJob = new ClientJob();
        log.info("--------Start, BusinessID = " + businessID + " Name = " + tempBus.getName() + " Time:" + currentTimestamp());
        // get email finder conditions
        Map<String, String> efConditions = getEmailFinderConditions(busID, connect);
        // get customers based on email finder conditions
        List<AppendJobDetail> result = getRecordsFromDBForBusID(tempBus, efConditions, connect);
        if (result.size() > 0) {
            // Insert a new AppendJob
            appendJobID = insertAppendJob(tempBus, connect);
            // update AppendJobDetail records
            updateAndInsertAppendJobDetail(result, appendJobID, busID, connect);
            // delete AppendJobDetail records from previous AppendJobs
            deleteOldAppendJobDetail(appendJobID, busID, connect);
            // create a Bluebox ClientJobDetail entry
            clientJob = createBlueboxClientJob(busID);
            // insert records to be process by Bluebox into ClientJobDetail
            insertClientJobDetailRecord(clientJob, result, connect);
            // update ClientJob sumbittedTS - This will trigger Bluebox to send record to Axciom
            updateBlueboxClientJob(clientJob);
            // update AppendJob  table with ExportCount SubmitDate and ClientJobID
            updateAppendJob(appendJobID, busID, clientJob.getId(),  connect);
        }

        log.info("--------Finish, BusinessID = " + businessID + " Name = " + tempBus.getName() + " Customers Exported: " + result.size()+ " Time:" + currentTimestamp());
        return result;
    }

    /*
     * Update AppendJob
     */
    private static void updateAppendJob(int ajID, int busID, int cjID, Connection connect) throws SQLException {
        PreparedStatement state = null;
        try {
            String sql = "update AppendJob set ExportCount = (select count(ajd.AppendJobDetailID) as countnum from AppendJobDetail as ajd where ajd.BusinessID = ? and AppendJobID = ? and ajd.CustomerID > 0), "
                    +" ClientJobId = ? "
                    +", SubmitDate = CURRENT_TIMESTAMP "
                    + " where AppendJobID = ? and BusinessID = ?";
            state = connect.prepareStatement(sql);
            state.setInt(1, busID);
            state.setInt(2, ajID);
            state.setInt(3, cjID);
            state.setInt(4, ajID);
            state.setInt(5, busID);
            int updateAppendJob = state.executeUpdate();
            log.info("Finish update AppendJob for BusinessID = " + busID + " AppendJobID = " + ajID + " result = " + updateAppendJob);
        } finally {
            DbUtils.closeQuietly(null, state, null);
        }
    }

    /*
     * Delete Old records from AppendJobDetail after New insert
     */
    private static void deleteOldAppendJobDetail(int ajID, int busID, Connection connect) throws SQLException  {
        PreparedStatement state = null;
        try {
            String sql = "DELETE FROM AppendJobDetail "
                    + " where AppendJobID < ? and BusinessID = ?";
            state = connect.prepareStatement(sql);
            state.setInt(1, ajID);
            state.setInt(2, busID);
            int deleteAppendJob = state.executeUpdate();
            log.info("Finish delete AppendJob for BusinessID = " + busID + " AppendJobID = " + ajID + " result = " + deleteAppendJob);
        } finally {
            DbUtils.closeQuietly(null, state, null);
        }
    }

    /*
	 * Get customer records on base of such conditions: 1. email finder
	 * conditions (can get from business portal setup/email finder page 2. email
	 * address is null or empty 3. status is 1 or 2 4. is not delete 5. has
	 * firstname or lastname 6. has no option 19 and value 1 or value -1 in
	 * customer options table 7. has no source = 1 in customer email history
	 * table
	 */
    private static List<AppendJobDetail> getRecordsFromDBForBusID(Business business, Map<String, String> efConditions, Connection connect) throws Exception {
        PreparedStatement cusState = null;
        ResultSet cusRS = null;
        List<AppendJobDetail> ajdList = new ArrayList<AppendJobDetail>();
        try {
            // TODO: Create Append Job for business
            int busID = business.getId().intValue();
            boolean onlyIndiv = isOnlyIndiv(connect, business);
            log.info("getRecordsFromDBForBusID.onlyIndiv: "+onlyIndiv);
            // get detail email finder conditions
            String sqlPrefix = " select cus.ID as cusid , cus.FirstName as cusFirstName, cus.LastName as cusLastName, cus.Address1 as cusAddress1, "
                    + "cus.Address2 as cusAddress2, cus.State as cusState, cus.City as cusCity, cus.ZipCode, cus.LastVisit ";
            cusState = initStatement(efConditions, connect, busID, sqlPrefix);
            log.debug(cusState.toString());
            cusRS = cusState.executeQuery();
            while (cusRS.next()) {
                AppendJobDetail lastRecord = new AppendJobDetail();
                lastRecord.setCustomerID(cusRS.getInt("cusid"));
                lastRecord.setBusinessID(busID);
                lastRecord.setMatchLevelRequest(onlyIndiv ? "I" : "H");
                ajdList.add(lastRecord);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error happened on NewEFExport.getRecordsFromDBForBusID:", e);
        } finally {
            DbUtils.closeQuietly(null, cusState, cusRS);
        }
        return ajdList;
    }

    private static boolean isOnlyIndiv(Connection connect, Business business) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean onlyIndiv = false;
        try {
            String query = "select optionvalue from BusinessOptions where optiontype = ? and businessId = ?";
            ps = connect.prepareStatement(query);
            ps.setInt(1, Constants.OPTION_ALWAYS_GET_INDIV_MATCH);
            ps.setLong(2, business.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                onlyIndiv = rs.getBoolean("optionvalue");
            } else {
                onlyIndiv = false;
            }
        } finally {
            DbUtils.closeQuietly(null, ps, rs);
        }
        return onlyIndiv;
    }

    /**
     * @param efConditions
     * @param connect
     * @param busID
     * @param sqlPrefix
     * @return
     * @throws SQLException
     */
    private static PreparedStatement initStatement(Map<String, String> efConditions, Connection connect, int busID, String sqlPrefix) throws SQLException {
        String lastVisitSQL = (String) efConditions.get(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "");
        lastVisitSQL = StringUtils.isEmpty(lastVisitSQL) ? "" : lastVisitSQL;
        String ageSQL = (String) efConditions.get(Constants.OPTION_EAPPEND_AGERESTRICTION + "");
        ageSQL = StringUtils.isEmpty(ageSQL) ? "" : ageSQL;
        String limitCount = (String) efConditions.get(Constants.OPTION_EAPPEND_MAX_MONTHLY_COLLECTION + "");
        String hasLimit = (String) efConditions.get("haslimit");
        log.info("BusinessID :" + busID + " age restriction:" + ageSQL + "  lastVisitSQL:" + lastVisitSQL + " has limit: " + hasLimit + " limit count: " + limitCount);
        String sql = sqlPrefix
                + "from Customer as cus "
                + "where cus.BusinessID = ?  and (cus.Status = ? or cus.Status = ?  or cus.Status = ?) "
                + "and (cus.Email is null or cus.Email = '') and cus.IsDeleted = 0 "
                + lastVisitSQL
                + ageSQL
                + " and cus.ZipCode != '' and cus.LastName != '' and (cus.Address1 != '' or cus.Address2 != ''  ) "
                + " and not exists (select 1 from CustomerEmailHistory as eh where eh.CustomerID = cus.ID and eh.Source = ? and eh.EmailID is not null and eh.EmailID <> '') "
                + " and not exists (select 1 from CustomerOptions as co where co.CustomerID = cus.ID and co.BusinessID = ? and co.OptionType = ? and co.OptionValue != '0') "
                + " and not exists (select 1 from CustomerType as ct, CustomerTypeCommunication as ctc where ct.businessid = ? and ctc.businessid = ? and ct.id = ctc.customertypeid and ct.value = cus.type and ctc.optiontype = ? and ctc.optionvalue = 0)"
                + " order by cus.LastVisit desc, cus.CreatedDate desc ";
        if (hasLimit.equals("1"))
            sql += " limit ?";
        PreparedStatement cusState = connect.prepareStatement(sql);
        log.debug(cusState.toString());
        cusState.setInt(1, busID);
        cusState.setInt(2, Constants.CUSTOMER_STATUS_PENDING_ACTIVE);
        cusState.setInt(3, Constants.CUSTOMER_STATUS_ACTIVE);
        cusState.setInt(4, Constants.CUSTOMER_STATUS_ACTIVE_OFFLINE);
        cusState.setInt(5, Constants.EMAIL_SOURCE_EAPPEND);
        cusState.setInt(6, busID);
        cusState.setInt(7, Constants.OPTION_EMAIL_ENABLED);
        cusState.setInt(8, busID);
        cusState.setInt(9, busID);
        cusState.setInt(10, Constants.CUSTOMER_OPTION_EMAIL_FINDER);
        if (hasLimit.equals("1")) {
            cusState.setInt(11, Integer.parseInt(limitCount));
        }
        return cusState;
    }

    private static int insertAppendJob(Business tempBus, Connection connect) throws SQLException, ParseException {
        PreparedStatement state = null;
        long key = -1L;
        StringBuffer str = new StringBuffer();
        try {
            str.append("insert into AppendJob set BusinessID=?, ");
            str.append("AppendType='EMAIL', CreatedDate=CURRENT_TIMESTAMP ");
            state = connect.prepareStatement(str.toString(), Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, tempBus.getId().intValue());
            log.debug(state.toString());
            state.executeUpdate();
            ResultSet rs = state.getGeneratedKeys();
            if (rs != null && rs.next()) {
                key = rs.getLong(1);
            }
            return (int) key;

        } finally {
            DbUtils.closeQuietly(state);
        }
    }

    /*
	 * 1. Update customers which already are in AppendJobDetail table (update first append)
	 * 2. Insert the new customers which are not in AppendJobDetail table (insert with new AppendJobID)
	 */
    private static void updateAndInsertAppendJobDetail(List<AppendJobDetail> records, int ajID, int busID, Connection updateConnect) throws SQLException, ParseException {
        if (records == null || records.size() == 0) {
            return;
        }
        PreparedStatement updateState = null;
        try {
            log.info("Begin update AppendJobDetail and insert AppendJobDetail table for BusinesssID = " + busID + " AppendJobID = " + ajID + " with " + records.size() + " records");
            for (AppendJobDetail record:records) {
                updateState = updateConnect.prepareStatement("INSERT INTO  AppendJobDetail (AppendJobID, BusinessID, CustomerID, MatchLevelRequest) " +
                        " values (?,?,?,?) " +
                        " ON DUPLICATE KEY UPDATE " +
                        " AppendJobID = "+ ajID + ", FirstAppend = 0");
                updateState.setInt(1, ajID);
                updateState.setInt(2, busID);
                updateState.setInt(3, record.getCustomerID());
                updateState.setString(4, record.getMatchLevelRequest());
                log.debug("updateAndInsertAppendJobDetail: ***SQL*** "+updateState.toString());
                updateState.executeUpdate();
            }
            log.info("End update AppendJobDetail and insert AppendJobDetail table for BusinesssID = " + busID + " AppendJobID = " + ajID);
        } finally {
            DbUtils.closeQuietly(null, updateState, null);
        }

    }

    private static Date midParase(String dateStr) throws ParseException {
        SimpleDateFormat df_mid = new SimpleDateFormat("yyyy-MM-dd");
        return df_mid.parse(dateStr);
    }


    static Business getBusiness(String busID, Connection connect) throws SQLException, ParseException {
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            state = connect.prepareStatement(QUERY_BUSINESS_EAPPEND_BY_ID);
            state.setInt(1, Constants.OPTION_EAPPEND_ON);
            state.setInt(2, Constants.EAPPEND_ON);
            state.setInt(3, Integer.parseInt(busID));
            rs = state.executeQuery();
            if (rs.next()) {
                return Business.getInstance(rs);
            } else {
                return null;
            }
        } finally {
            DbUtils.closeQuietly(null, state, rs);
        }
    }

    static List<Business> getAllExportedBusiness(Connection connect, String businessID) throws SQLException, ParseException {
        PreparedStatement state = null;
        ResultSet rs = null;
        List<Business> businessList = new ArrayList<Business>();
        int busID = 0;
        if (!businessID.equals("All")) busID = Integer.parseInt(businessID);
        try {
            if (busID == 0) {
                state = connect.prepareStatement(QUERY_BUSINESS_EAPPEND);
                state.setInt(1, Constants.OPTION_EAPPEND_ON);
                state.setInt(2, Constants.EAPPEND_ON);
                rs = state.executeQuery();
                int count = 0;
                while (rs.next()) {
                    log.info("Business: "+ rs.getInt(1));
                    businessList.add(Business.getInstance(rs));
                    count++;
                }
                log.info("Total Businesses: "+count);
            } else {
                state = connect.prepareStatement(QUERY_BUSINESS_EAPPEND_CUSTOM);
                state.setInt(1, Constants.OPTION_EAPPEND_ON);
                state.setInt(2, Constants.EAPPEND_ON);
                state.setInt(3, busID);
                log.debug(state.toString());
                rs = state.executeQuery();
                int count = 0;
                while (rs.next()) {
                    log.info("Business: "+ rs.getInt(1));
                    businessList.add(Business.getInstance(rs));
                    count++;
                }
                log.info("Total Businesses: "+count);
            }
        } finally {
            DbUtils.closeQuietly(null, state, rs);
        }
        return businessList;
    }

    public static Map<String, String> getEmailFinderConditions(int businessID, Connection connect) {
        Map<String, String> conditions = new HashMap<String, String>();
        // set the default value to avoid null pointer
        conditions.put("haslimit", "0");
        conditions.put(Constants.OPTION_EAPPEND_MAX_MONTHLY_COLLECTION + "", "0");
        PreparedStatement conditionsState = null;
        ResultSet conditionsRS = null;
        int conditionValue = -1;
        String ageSQL = "";
        try {
            conditionsState = connect.prepareStatement("select OptionType, OptionValue, LastModifiedUserID from BusinessOptions where BusinessID = ?");
            conditionsState.setInt(1, businessID);
            conditionsRS = conditionsState.executeQuery();
            String optionValue = null;
            while (conditionsRS.next()) {
                optionValue = conditionsRS.getString("OptionValue");
                // LastModifiedUserID
                if (conditionsRS.getInt("OptionType") == Constants.OPTION_EAPPEND_ON) {
                    optionValue = conditionsRS.getString("LastModifiedUserID");
                    conditions.put(Constants.OPTION_EAPPEND_ON + "", (StringUtils.isEmpty(optionValue)) ? "0" : optionValue);
                }
                // EAPPEND_CUSTOMER_STATUS
                if (conditionsRS.getInt("OptionType") == Constants.OPTION_EAPPEND_CUSTOMER_STATUS) {
                    conditionValue = (StringUtils.isEmpty(optionValue)) ? -1 : Integer.parseInt(optionValue);
                    switch (conditionValue) {
                        case Constants.EAPPEND_CUSTOMER_STATUS_ALL:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "", "");
                            conditions.put("customerstatus", "All");
                            break;
                        case Constants.EAPPEND_CUSTOMER_STATUS_WITHIN_18:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "", " and cus.LastVisit > date_sub(now(),interval 18 month) and cus.LastVisit < now() ");
                            conditions.put("customerstatus", "Last visit fewer than 18 months");
                            break;
                        case Constants.EAPPEND_CUSTOMER_STATUS_WITHIN_36:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "",
                                    " and cus.LastVisit >= date_sub(now(),interval 36 month) and cus.LastVisit  <= date_sub(now(),interval 18 month) ");
                            conditions.put("customerstatus", "Last visit 18 - 36 months");
                            break;
                        case Constants.EAPPEND_CUSTOMER_STATUS_MORE_THAN_36:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "", " and cus.LastVisit < date_sub(now(),interval 36 month) ");
                            conditions.put("customerstatus", "Last visit more than 36 months");
                            break;
                        case Constants.EAPPEND_CUSTOMER_STATUS_FEWER_THAN_36:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "", " and cus.LastVisit > date_sub(now(),interval 36 month) and cus.LastVisit < now() ");
                            conditions.put("customerstatus", "Last visit fewer than 36 months");
                            break;
                        default:
                            conditions.put(Constants.OPTION_EAPPEND_CUSTOMER_STATUS + "", "");
                            conditions.put("customerstatus", "All");
                            break;
                    }
                }
                // EAPPEND_AGERESTRICTION
                if (conditionsRS.getInt("OptionType") == Constants.OPTION_EAPPEND_AGERESTRICTION) {
                    conditionValue = (StringUtils.isEmpty(optionValue)) ? 0 : Integer.parseInt(optionValue);
                    ageSQL = " and (select if(YEAR(cus.Birthday) >= 1920, " + "(case when (MONTH(cus.Birthday) = MONTH(now())) then "
                            + "(if(DAY(cus.Birthday) <= DAY(now()), YEAR(now())-YEAR(cus.Birthday), YEAR(now())-YEAR(cus.Birthday)-1)) else "
                            + "if(MONTH(cus.Birthday) > MONTH(now()), YEAR(now())-YEAR(cus.Birthday)-1, YEAR(now())-YEAR(cus.Birthday)) end), -1) as age) >= " + conditionValue;
                    conditions.put(Constants.OPTION_EAPPEND_AGERESTRICTION + "", ((conditionValue > 0) ? ageSQL : ""));
                    switch (conditionValue) {
                        case Constants.EAPPEND_AGERESTRICTION_ALL:
                            conditions.put("agerestriction", "All");
                            break;
                        case Constants.EAPPEND_AGERESTRICTION_16:
                            conditions.put("agerestriction", "16 and Older");
                            break;
                        case Constants.EAPPEND_AGERESTRICTION_18:
                            conditions.put("agerestriction", "18 and Older");
                            break;
                        default:
                            conditions.put("agerestriction", "All");
                            break;
                    }
                }
                // EAPPEND_MAX_MONTHLY_COLLECTION
                if (conditionsRS.getInt("OptionType") == Constants.OPTION_EAPPEND_MAX_MONTHLY_COLLECTION) {
                    conditionValue = (StringUtils.isEmpty(optionValue)) ? 0 : Integer.parseInt(optionValue);
                    conditions.put(Constants.OPTION_EAPPEND_MAX_MONTHLY_COLLECTION + "", conditionValue + "");
                    conditions.put("haslimit", conditionValue == 0 ? "0" : "1");
                    if (conditionValue == Constants.EAPPEND_MAX_MONTHLY_COLLECTION_ALL)
                        conditions.put("maximuncount", "All");
                    else
                        conditions.put("maximuncount", (conditionValue + " records"));

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Can not get email finder conditions : " + e.getMessage(), e);
        } finally {
            DbUtils.closeQuietly(null, conditionsState, conditionsRS);
        }
        return conditions;
    }

    public static void insertClientJobDetailRecord(ClientJob clientJob, List<AppendJobDetail> customers, Connection connect) {
        int count = 0;
        final String acceptHeader = "application/json";
        try {
            for (AppendJobDetail customer:customers) {
                ClientJobDetail cjd = new ClientJobDetail();
                Customer d3customer = getCustomerDetails(customer.getCustomerID(), customer.getBusinessID(), connect);
                if (d3customer == null) {
                    continue;
                }
                cjd.setClientJobid(clientJob.getId());
                cjd.setConsumerid(customer.getCustomerID());
                cjd.setFname(d3customer.getFirstName());
                cjd.setLname(d3customer.getLastName());
                cjd.setAddress1(d3customer.getAddress1());
                cjd.setCity(d3customer.getCity());
                cjd.setState(d3customer.getState());
                cjd.setZip(d3customer.getZipCode());
                cjd.setPrecisionReq(customer.getMatchLevelRequest());

                // Send Rest request to bluebox
                HttpClient httpClient = RestClient.httpClientAuth();
                PostMethod method = new PostMethod(_clientJobURL + "/" + clientJob.getId() + "/clientJobDetail");
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String body = mapper.writeValueAsString(cjd);
                method.setRequestHeader("Accept", acceptHeader);
                method.setRequestEntity(new StringRequestEntity(body, "application/json", "US-ASCII"));
                int status = httpClient.executeMethod(method);
                if (status == 201) {
                    log.info("Customer sent to Bluebox for processing - CustomerID =  " + cjd.getConsumerid() + ", Name = " + cjd.getFname() + " " + cjd.getLname() + " Status = " + status + " Count = " +  ++count);
                } else {
                    log.info("ERROR: Couldn't send Customer to Bluebox for processing - CustomerID =  " + cjd.getConsumerid() + ", Name = " + cjd.getFname() + " " + cjd.getLname() + " Status = " + status + " Count = " +  ++count);
                }
                method.releaseConnection();
            }
        } catch (Exception e) {
            log.error("Error inserting Customers into Bluebox " + e);
        }
    }

    public static Customer getCustomerDetails(int custID, int busID, Connection connect) {
        PreparedStatement state = null;
        ResultSet rs = null;
        Customer customer = new Customer();
        try {
            String sql = " select ID, FirstName, LastName, Address1, "
                    + "State, City, ZipCode from Customer where ID  = ? and BusinessID = ?";
            state = connect.prepareStatement(sql);
            state.setInt(1, custID);
            state.setInt(2, busID);
            log.debug(state.toString());
            rs = state.executeQuery();
            while (rs.next()) {
                customer = new Customer();
                customer.setId(rs.getInt("ID"));
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setAddress1(rs.getString("Address1"));
                customer.setState(rs.getString("State"));
                customer.setCity(rs.getString("City"));
                customer.setZipCode(rs.getString("ZipCode"));
            }
        } catch (SQLException e) {
            log.error("SQL Exception: " + e);
        } finally {
            DbUtils.closeQuietly(null, state, rs);
        }
        return customer;
    }

    public static void updateBlueboxClientJob(ClientJob clientJob) throws NamingException {
        final String acceptHeader = "application/json";
        try {
            HttpClient httpClient = RestClient.httpClientAuth();
            PutMethod method = new PutMethod(_clientJobURL + "/" + clientJob.getId());
            clientJob.setSubmitTs(new Timestamp(new java.util.Date().getTime()));
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String body = mapper.writeValueAsString(clientJob);
            method.setRequestHeader("Accept", acceptHeader);
            method.setRequestEntity(new StringRequestEntity(body, "application/json", "US-ASCII"));
            int result = httpClient.executeMethod(method);
            method.releaseConnection();
            log.info("ClientJob ID "+ clientJob.getId() + " submitted Result = " + result);
        } catch (Exception e) {
            log.error("ERROR: updateBlueboxClientJob  ", e);
        }

    }

    public static ClientJob createBlueboxClientJob(int busID) throws NamingException {
        final String acceptHeader = "application/json";
        Client client = httpClient();
        ClientJob clientJob = new ClientJob(client.getId(), busID);
        log.debug(clientJob);
        try {
            HttpClient httpClient = RestClient.httpClientAuth();
            PostMethod method = new PostMethod(_clientJobURL);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String body = mapper.writeValueAsString(clientJob);
            log.debug(body);
            method.setRequestHeader("Accept", acceptHeader);
            method.setRequestEntity(new StringRequestEntity(body, "application/json", "US-ASCII"));
            httpClient.executeMethod(method);
            String responseBody = new String(method.getResponseBody());
            clientJob = new ObjectMapper().readValue(responseBody, ClientJob.class);
            log.info("createBlueboxClientJob result Object:" + clientJob);
            method.releaseConnection();
        } catch (IOException e) {
            log.error("ERROR: createBlueboxClientJob ", e);
        }
        return clientJob;
    }

    public static final Client httpClient() throws NamingException {
        Client result = new Client();
        final String acceptHeader = "application/json";
        try {
            HttpClient httpClient = RestClient.httpClientAuth();
            HttpMethod httpMethod = new GetMethod(_clientURl);
            httpMethod.setRequestHeader("Accept", acceptHeader);
            httpClient.executeMethod(httpMethod);
            String responseBody = new String(httpMethod.getResponseBody());
            httpMethod.releaseConnection();

            Client[] clients = new ObjectMapper().readValue(responseBody, Client[].class);

            System.out.println("responseBody: " + responseBody);
            for(Client client : clients) {
                log.debug("client: " + client);
                result = client;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /*
     * Testing purposes in ng30
     * @param businessID 130000054
     * @param nodeName devng30
     */
    // TODO Get the node name from the business object instead of passing it as a parameter
    public static void main(String[] args) {
        log.info("***New EF Export Begin.");
        Connection conn = null;
        PreparedStatement ps;
        ResultSet rs;
        Connection nodeConn = null;
        String businessToProcess;
        String nodeName;
        try {
            if (args.length == 2) {
                businessToProcess = args[0];
                nodeName = args[1];
                try {
                    nodeConn = DBUtils.getConnection(nodeName);
                    exportNode(businessToProcess, nodeName, nodeConn);
                } finally {
                    if (nodeConn != null) {
                        DbUtils.closeQuietly(nodeConn);
                    }
                }
            } else {
                conn = DBUtils.getConnection("jdbc/cluster");
                ps = conn.prepareStatement("select NodeGroup from cluster.Business group by NodeGroup");
                rs = ps.executeQuery();
                log.info("Node List:");
                while (rs.next()) {
                    log.info(rs.getString("NodeGroup"));
                    String ng = rs.getString("NodeGroup");
                    try {
                        nodeConn = DBUtils.getConnection(ng);
                        exportNode("All", ng, nodeConn);
                    } finally {
                       if (nodeConn != null) {
                            DbUtils.closeQuietly(nodeConn);
                       }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }
}
