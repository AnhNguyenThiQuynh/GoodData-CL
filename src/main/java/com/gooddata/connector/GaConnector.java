package com.gooddata.connector;

import au.com.bytecode.opencsv.CSVWriter;
import com.gooddata.exception.*;
import com.gooddata.google.analytics.FeedDumper;
import com.gooddata.google.analytics.GaQuery;
import com.gooddata.modeling.model.SourceColumn;
import com.gooddata.modeling.model.SourceSchema;
import com.gooddata.processor.CliParams;
import com.gooddata.processor.Command;
import com.gooddata.util.FileUtil;
import com.google.gdata.client.analytics.AnalyticsService;
import com.google.gdata.data.analytics.DataFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import org.apache.log4j.Logger;
import org.gooddata.connector.AbstractConnector;
import org.gooddata.connector.Connector;
import org.gooddata.connector.backend.ConnectorBackend;
import com.gooddata.processor.ProcessingContext;

import java.io.*;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * GoodData Google Analytics Connector
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class GaConnector extends AbstractConnector implements Connector {

    public static final String GA_DATE = "ga:date";
    
    private static Logger l = Logger.getLogger(GaConnector.class);

    private static final String APP_NAME = "gdc-ga-client";

    private String googleAnalyticsUsername;
    private String googleAnalyticsPassword;
    private GaQuery googleAnalyticsQuery;

    /**
     * Creates a new Google Analytics Connector
     * @param connectorBackend connector backend
     */
    protected GaConnector(ConnectorBackend connectorBackend) {
        super(connectorBackend);
    }

     /**
     * Creates a new Google Analytics Connector
     * @param connectorBackend connector backend
     */
    public static GaConnector createConnector(ConnectorBackend connectorBackend) {
        return new GaConnector(connectorBackend);
    }

    /**
     * Saves a template of the config file
     * @param name the new config file name 
     * @param configFileName the new config file name
     * @param gQuery the Google Analytics query
     * @throws com.gooddata.exception.InvalidArgumentException if there is a problem with arguments
     * @throws IOException if there is a problem with writing the config file
     */
    public static void saveConfigTemplate(String name, String configFileName, GaQuery gQuery)
            throws InvalidArgumentException, IOException {
        String dims = gQuery.getDimensions();
        String mtrs = gQuery.getMetrics();
        SourceSchema s = SourceSchema.createSchema(name);
        if(dims != null && dims.length() > 0) {
            String[] dimensions = dims.split("\\|");
            for(String dim : dimensions) {
                // remove the "ga:"
                if(dim != null && dim.length() > 3) {
                    String d= dim.substring(3);
                    if(GA_DATE.equals(dim)) {
                        SourceColumn sc = new SourceColumn(d,SourceColumn.LDM_TYPE_DATE, d);
                        sc.setFormat("yyyy-MM-dd");
                        s.addColumn(sc);
                    }
                    else {
                        SourceColumn sc = new SourceColumn(d,SourceColumn.LDM_TYPE_ATTRIBUTE, d);
                        s.addColumn(sc);
                    }
                }
                else {
                    l.error("Invalid dimension name '" + dim + "'");
                    throw new InvalidArgumentException("Invalid dimension name '" + dim + "'");
                }
            }
        }
        else {
            l.error("Please specify Google Analytics dimensions separated by comma.");
            throw new InvalidArgumentException("Please specify Google Analytics dimensions separated by comma.");            
        }
        if(mtrs != null && mtrs.length() > 0) {
            String[] metrics = mtrs.split("\\|");
            for(String mtr : metrics) {
                // remove the "ga:"
                if(mtr != null && mtr.length() > 3) {
                    String m= mtr.substring(3);
                    SourceColumn sc = new SourceColumn(m,SourceColumn.LDM_TYPE_FACT, m);
                    s.addColumn(sc);
                }
                else {
                    l.error("Invalid dimension name '" + mtr + "'");
                    throw new InvalidArgumentException("Invalid metric name '" + mtr + "'");
                }
            }
        }
        else {
            l.error("Please specify Google Analytics metrics separated by comma.");
            throw new InvalidArgumentException("Please specify Google Analytics metrics separated by comma.");
        }
        s.writeConfig(new File(configFileName));
    }

    /**
     * {@inheritDoc}
     */
    public void extract() throws IOException {
        Connection con = null;
        try {
            AnalyticsService as = new AnalyticsService(APP_NAME);
		    as.setUserCredentials(getGoogleAnalyticsUsername(), getGoogleAnalyticsPassword());
            File dataFile = FileUtil.getTempFile();
            GaQuery gaq = getGoogleAnalyticsQuery();
            gaq.setMaxResults(5000);
            int cnt = 1;
            CSVWriter cw = new CSVWriter(new FileWriter(dataFile));
            for(int startIndex = 1; cnt > 0; startIndex += cnt + 1) {
                gaq.setStartIndex(startIndex);
                DataFeed feed = as.getFeed(gaq.getUrl(), DataFeed.class);
                l.debug("Retrieving GA data from index="+startIndex);
                cnt = FeedDumper.dump(cw, feed);
                l.debug("Retrieved "+cnt+" entries.");
            }
            cw.flush();
            cw.close();
            getConnectorBackend().extract(dataFile);
            FileUtil.recursiveDelete(dataFile);
        }
        catch (AuthenticationException e) {
            throw new InternalError(e.getMessage());
        } catch (ServiceException e) {
            throw new InternalError(e.getMessage());
        } finally {
            try {
                if (con != null && !con.isClosed())
                    con.close();
            }
            catch (SQLException e) {
                throw new InternalError(e.getMessage());
            }
        }
    }

    /**
     * Google Analytics username getter
     * @return Google Analytics username
     */
    public String getGoogleAnalyticsUsername() {
        return googleAnalyticsUsername;
    }

    /**
     * Google Analytics username setter
     * @param googleAnalyticsUsername Google Analytics username
     */
    public void setGoogleAnalyticsUsername(String googleAnalyticsUsername) {
        this.googleAnalyticsUsername = googleAnalyticsUsername;
    }

    /**
     * Google Analytics password getter
     * @return Google Analytics password
     */
    public String getGoogleAnalyticsPassword() {
        return googleAnalyticsPassword;
    }

    /**
     * Google Analytics password setter
     * @param googleAnalyticsPassword Google Analytics password
     */
    public void setGoogleAnalyticsPassword(String googleAnalyticsPassword) {
        this.googleAnalyticsPassword = googleAnalyticsPassword;
    }

    /**
     * Google Analytics query getter
     * @return Google Analytics query
     */
    public GaQuery getGoogleAnalyticsQuery() {
        return googleAnalyticsQuery;
    }

    /**
     * Google Analytics query setter
     * @param googleAnalyticsQuery Google Analytics query
     */
    public void setGoogleAnalyticsQuery(GaQuery googleAnalyticsQuery) {
        this.googleAnalyticsQuery = googleAnalyticsQuery;
    }

    /**
     * {@inheritDoc}
     */
    public boolean processCommand(Command c, CliParams cli, ProcessingContext ctx) throws ProcessingException {
        try {
            if(c.match("GenerateGoogleAnalyticsConfig")) {
                generateGAConfig(c, cli, ctx);
            }
            else if(c.match("LoadGoogleAnalytics")) {
                loadGA(c, cli, ctx);
            }
            else
                return super.processCommand(c, cli, ctx);
        }
        catch (IOException e) {
            throw new ProcessingException(e);
        }
        return true;
    }

    /**
     * Loads new GA data command processor
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void loadGA(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        GaQuery gq = null;
        try {
            gq = new GaQuery();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        String configFile = c.getParamMandatory("configFile");
        String usr = c.getParamMandatory("username");
        String psw = c.getParamMandatory("password");
        String id = c.getParamMandatory("profileId");
        File conf = FileUtil.getFile(configFile);
        initSchema(conf.getAbsolutePath());
        gq.setIds(id);
        setGoogleAnalyticsUsername(usr);
        setGoogleAnalyticsPassword(psw);
        setGoogleAnalyticsQuery(gq);
        gq.setDimensions(c.getParamMandatory("dimensions").replace("|",","));
        gq.setMetrics(c.getParamMandatory("metrics").replace("|",","));
        gq.setStartDate(c.getParamMandatory("startDate"));
        gq.setEndDate(c.getParamMandatory("endDate"));
        if(c.checkParam("filters"))
            gq.setFilters(c.getParam("filters"));
        // sets the current connector
        ctx.setConnector(this);
        setProjectId(ctx);
    }

    /**
     * Generate GA config command processor
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void generateGAConfig(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        String configFile = c.getParamMandatory("configFile");
        String name = c.getParamMandatory("name");
        String dimensions = c.getParamMandatory("dimensions");
        String metrics = c.getParamMandatory("metrics");
        File cf = new File(configFile);
        GaQuery gq = null;
        try {
            gq = new GaQuery();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        gq.setDimensions(dimensions);
        gq.setMetrics(metrics);
        GaConnector.saveConfigTemplate(name, configFile, gq);
    }

}
