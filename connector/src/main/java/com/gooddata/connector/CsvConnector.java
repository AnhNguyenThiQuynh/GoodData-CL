/*
 * Copyright (c) 2009, GoodData Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and
 *        the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *        and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the GoodData Corporation nor the names of its contributors may be used to endorse
 *        or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gooddata.connector;

import java.io.File;
import java.io.IOException;

import org.gooddata.connector.AbstractConnector;
import org.gooddata.connector.Connector;
import org.gooddata.connector.backend.ConnectorBackend;
import org.gooddata.processor.CliParams;
import org.gooddata.processor.Command;
import org.gooddata.processor.ProcessingContext;

import com.gooddata.connector.driver.Constants;
import com.gooddata.exception.ProcessingException;
import com.gooddata.modeling.model.SourceColumn;
import com.gooddata.modeling.model.SourceSchema;
import com.gooddata.util.FileUtil;
import com.gooddata.util.NameTransformer;
import com.gooddata.util.StringUtil;

/**
 * GoodData CSV Connector
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class CsvConnector extends AbstractConnector implements Connector {

    
    // data file
    private File dataFile;
    
     //hasheader flag
    private boolean hasHeader;

    /**
     * Creates GoodData CSV connector
     * @param backend connector backend
     */
    protected CsvConnector(ConnectorBackend backend) {
        super(backend);
    }

    /**
     * Create a new GoodData CSV connector. This constructor creates the connector from a config file
     * @param backend connector backend
     * @return new CSV Connector
     */
    public static CsvConnector createConnector(ConnectorBackend backend) {
        return new CsvConnector(backend);    
    }

    /**
     * Saves a template of the config file
     * @param configFileName the new config file name
     * @param dataFileName the data file
     * @throws IOException in case of an IO error
     */
    public static void saveConfigTemplate(String configFileName, String dataFileName) throws IOException {
    	saveConfigTemplate(configFileName, dataFileName, null, null);
    }

    /**
     * Saves a template of the config file
     * @param configFileName the new config file name
     * @param dataFileName the data file
     * @param defaultLdmType default LDM type
     * @param folder default folder
     * @throws IOException in case of an IO issue
     */
    public static void saveConfigTemplate(String configFileName, String dataFileName, String defaultLdmType, String folder) throws IOException {
        File dataFile = new File(dataFileName);
        String name = dataFile.getName().split("\\.")[0];
        String[] headers = FileUtil.getCsvHeader(dataFile);
        int i = 0;
        final SourceSchema s;
        File configFile = new File(configFileName);
        if (configFile.exists()) {
        	s = SourceSchema.createSchema(configFile);
        } else {
        	s = SourceSchema.createSchema(name);
        }
        final int knownColumns = s.getColumns().size();
        NameTransformer idGen = new NameTransformer(new NameTransformer.NameTransformerCallback() {
        	public String transform(String str) {
        		String idorig = StringUtil.csvHeaderToIdentifier(str);
        		int idmax = Constants.MAX_TABLE_NAME_LENGTH - s.getName().length() - 3; // good enough for 999 long names
        		if (idorig.length() <= idmax)
        			return idorig;
        		return idorig.substring(0, idmax);
        		
        	}
        });
        NameTransformer titleGen = new NameTransformer(new NameTransformer.NameTransformerCallback() {
        	public String transform(String str) {
        		return StringUtil.csvHeaderToTitle(str);
        	}
        });
        for(int j = knownColumns; j < headers.length; j++) {
        	final String header = headers[j];
            final SourceColumn sc;
            final String identifier = idGen.transform(header);
            final String title = titleGen.transform(header);
            if (defaultLdmType != null) {
            	sc = new SourceColumn(identifier, defaultLdmType, title, folder);
            } else {
	            switch (i %3) {
	                case 0:
	                    sc = new SourceColumn(identifier, SourceColumn.LDM_TYPE_ATTRIBUTE, title, "folder");
	                    break;
	                case 1:
	                    sc = new SourceColumn(identifier, SourceColumn.LDM_TYPE_FACT, title, "folder");
	                    break;
	                case 2:
	                    sc = new SourceColumn(identifier, SourceColumn.LDM_TYPE_LABEL, title, "folder", "existing-attribute-name");
	                    break;
	                default:
	                	throw new AssertionError("i % 3 outside {0, 1, 2} - this cannot happen");
	            }
            }
            s.addColumn(sc);
            i++;
        }
        s.writeConfig(new File(configFileName));
    }

    /**
     * Data CSV file getter
     * @return the data CSV file
     */
    public File getDataFile() {
        return dataFile;
    }

    /**
     * Data CSV file setter
     * @param dataFile the data CSV file
     */
    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * Extracts the source data CSV to the Derby database where it is going to be transformed
     * @throws IOException in case of IO issues
     */
    public void extract() throws IOException {
        if(getHasHeader()) {
            File tmp = FileUtil.stripCsvHeader(getDataFile());
            getConnectorBackend().extract(tmp);
            tmp.delete();
        }
    }

    /**
     * hasHeader getter
     * @return hasHeader flag
     */
    public boolean getHasHeader() {
        return hasHeader;
    }

    /**
     * hasHeader setter
     * @param hasHeader flag value
     */
    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    /**
     * {@inheritDoc}
     */
    public boolean processCommand(Command c, CliParams cli, ProcessingContext ctx) throws ProcessingException {
        try {
            if(c.match("GenerateCsvConfig")) {
                generateCsvConfig(c, cli, ctx);
            }
            else if(c.match("LoadCsv")) {
                loadCsv(c, cli, ctx);
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
     * Loads new CSV file command processor
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void loadCsv(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        String configFile = c.getParamMandatory("configFile");
        String csvDataFile = c.getParamMandatory("csvDataFile");
        String hdr = c.getParamMandatory("header");
        File conf = FileUtil.getFile(configFile);
        File csvf = FileUtil.getFile(csvDataFile);
        boolean hasHeader = false;
        if(hdr.equalsIgnoreCase("true"))
            hasHeader = true;
        initSchema(conf.getAbsolutePath());
        setDataFile(new File(csvf.getAbsolutePath()));
        setHasHeader(hasHeader);
        // sets the current connector
        ctx.setConnector(this);
        setProjectId(ctx);
    }

    /**
     * Generate new config file from CSV command processor
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void generateCsvConfig(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        String configFile = c.getParamMandatory("configFile");
        String csvHeaderFile = c.getParamMandatory("csvHeaderFile");
        String defaultLdmType = c.getParam( "defaultLdmType");
    	String folder = c.getParam( "defaultFolder");
        
        CsvConnector.saveConfigTemplate(configFile, csvHeaderFile, defaultLdmType, folder);
    }
}