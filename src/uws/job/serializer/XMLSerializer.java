package uws.job.serializer;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI) 
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import uws.ISO8601Format;
import uws.job.ErrorSummary;
import uws.job.JobList;
import uws.job.Result;
import uws.job.UWSJob;
import uws.job.serializer.filter.JobListFilter;
import uws.job.user.JobOwner;
import uws.service.UWS;
import uws.service.UWSUrl;
import uws.service.request.UploadFile;

/**
 * Lets serializing any UWS resource in XML.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.2 (10/2015)
 */
public class XMLSerializer extends UWSSerializer {
	private static final long serialVersionUID = 1L;

	/** Version of the currently used UWS protocol. */
	public final static String UWS_VERSION = "1.1";

	/** Tab to add just before each next XML node. */
	protected String tabPrefix = "";

	/** The path of the XSLT style-sheet. */
	protected String xsltPath = null;

	/**
	 * Builds a XML serializer.
	 */
	public XMLSerializer(){
		;
	}

	/**
	 * Builds a XML serializer with a XSLT link.
	 * 
	 * @param xsltPath	Path of a XSLT style-sheet.
	 */
	public XMLSerializer(final String xsltPath){
		this.xsltPath = xsltPath;
	}

	/**
	 * Gets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @return	XSLT path/url.
	 */
	public final String getXSLTPath(){
		return xsltPath;
	}

	/**
	 * Sets the path/URL of the XSLT style-sheet to use.
	 * 
	 * @param path	The new XSLT path/URL.
	 */
	public final void setXSLTPath(final String path){
		if (path == null)
			xsltPath = null;
		else{
			xsltPath = path.trim();
			if (xsltPath.isEmpty())
				xsltPath = null;
		}
	}

	/**
	 * <p>Gets the XML file header (xml version, encoding and the xslt style-sheet link if any).</p>
	 * <p>It is always called by the implementation of the UWSSerializer functions
	 * if their boolean parameter (<i>root</i>) is <i>true</i>.</p>
	 * 
	 * @return	The XML file header.
	 */
	public String getHeader(){
		StringBuffer xmlHeader = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		if (xsltPath != null)
			xmlHeader.append("<?xml-stylesheet type=\"text/xsl\" href=\"").append(escapeXMLAttribute(xsltPath)).append("\"?>\n");
		return xmlHeader.toString();
	}

	/**
	 * Gets all UWS namespaces declarations needed for an XML representation of a UWS object.
	 * 
	 * @return	The UWS namespaces: <br /> (i.e. <i>= "xmlns:uws=[...] xmlns:xlink=[...] xmlns:xs=[...] xmlns:xsi=[...] xsi:schemaLocation=[...]"</i>).
	 */
	public String getUWSNamespace(){
		return "xmlns=\"http://www.ivoa.net/xml/UWS/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/UWS/v1.0 http://www.ivoa.net/xml/UWS/v1.0 http://www.w3.org/1999/xlink http://www.w3.org/1999/xlink.xsd http://www.w3.org/2001/XMLSchema http://www.w3.org/2001/XMLSchema.xsd\"";
	}

	/**
	 * Gets the node attributes which declare the UWS namespace.
	 * 
	 * @param root	<i>false</i> if the attribute to serialize will be included
	 * 				in a top level serialization (for a job attribute: job), <i>true</i> otherwise.
	 * 
	 * @return		"" if <i>root</i> is <i>false</i>, " "+UWSNamespace otherwise.
	 * 
	 * @see #getUWSNamespace()
	 */
	protected final String getUWSNamespace(boolean root){
		if (root)
			return " " + getUWSNamespace();
		else
			return "";
	}

	@Override
	public final String getMimeType(){
		return MIME_TYPE_XML;
	}

	@Override
	public String getUWS(final UWS uws, final JobOwner user){
		String name = uws.getName(), description = uws.getDescription();
		StringBuffer xml = new StringBuffer(getHeader());

		xml.append("<uws version=\"").append(UWS_VERSION).append('"').append(getUWSNamespace(true));
		if (name != null)
			xml.append(" name=\"").append(escapeXMLAttribute(name)).append('"');
		xml.append(">\n");

		if (description != null)
			xml.append("\t<description>\n").append(escapeXMLData(description)).append("\n\t</description>\n");

		xml.append("\t<jobLists>\n");
		for(JobList jobList : uws){
			UWSUrl jlUrl = jobList.getUrl();
			xml.append("\t\t<jobListRef name=\"").append(escapeXMLAttribute(jobList.getName())).append("\" href=\"");
			if (jlUrl != null && jlUrl.getRequestURL() != null)
				xml.append(escapeXMLAttribute(jlUrl.getRequestURL()));
			xml.append("\" />\n");
		}
		xml.append("\t</jobLists>\n");

		xml.append("</uws>\n");

		return xml.toString();
	}

	@Override
	public String getJobList(final JobList jobsList, JobOwner owner, final JobListFilter filter, final boolean root) throws Exception{
		StringBuffer xml = new StringBuffer(getHeader());

		xml.append("<jobs version=\"").append(UWS_VERSION).append('"').append(getUWSNamespace(true));
		/* NOTE: NO ATTRIBUTE "name" IN THE XML SCHEMA!
		 * String name = jobsList.getName();
		 * if (name != null)
		 * 	xml.append(" name=\"").append(escapeXMLAttribute(name)).append("\"");
		 */
		xml.append('>');

		UWSUrl jobsListUrl = jobsList.getUrl();

		// Security filter: retrieve only the jobs of the specified owner:
		Iterator<UWSJob> it = jobsList.getJobs(owner);

		// User filter: filter the jobs in function of filters specified by the user:
		if (filter != null)
			it = filter.filter(it);

		// Append the jobs' description:
		while(it.hasNext())
			xml.append("\n\t").append(getJobRef(it.next(), jobsListUrl));

		xml.append("\n</jobs>");

		return xml.toString();
	}

	@Override
	public String getJob(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		String newLine = "\n\t";

		// general information:
		xml.append("<job version=\"").append(UWS_VERSION).append('"').append(getUWSNamespace(root)).append('>');
		xml.append(newLine).append(getJobID(job, false));
		if (job.getRunId() != null)
			xml.append(newLine).append(getRunID(job, false));
		xml.append(newLine).append(getOwnerID(job, false));
		xml.append(newLine).append(getPhase(job, false));
		xml.append(newLine).append(getQuote(job, false));
		xml.append(newLine).append(getStartTime(job, false));
		xml.append(newLine).append(getEndTime(job, false));
		xml.append(newLine).append(getExecutionDuration(job, false));
		xml.append(newLine).append(getDestructionTime(job, false));

		tabPrefix = "\t";
		newLine = "\n";

		// parameters:
		xml.append(newLine).append(getAdditionalParameters(job, false));

		// results:
		xml.append(newLine).append(getResults(job, false));

		// errorSummary:
		xml.append(newLine).append(getErrorSummary(job.getErrorSummary(), false));

		// jobInfo:
		if (job.hasJobInfo())
			xml.append(newLine).append(getJobInfo(job, false));

		tabPrefix = "";
		return xml.append("\n</job>").toString();
	}

	@Override
	public String getJobRef(final UWSJob job, final UWSUrl jobsListUrl){
		String url = null;
		if (jobsListUrl != null){
			jobsListUrl.setJobId(job.getJobId());
			url = jobsListUrl.getRequestURL();
		}

		StringBuffer xml = new StringBuffer("<jobref id=\"");
		xml.append(escapeXMLAttribute(job.getJobId())).append('"');
		/* NOTE: NO ATTRIBUTE "runId" IN THE XML SCHEMA!
		 * if (job.getRunId() != null && job.getRunId().length() > 0)
		 * 	xml.append("\" runId=\"").append(escapeXMLAttribute(job.getRunId()));
		 */

		// The XLink attributes are optional. So if no URL is available for this Job reference, none is written here:
		if (url != null)
			xml.append(" xlink:type=\"simple\" xlink:href=\"").append(escapeXMLAttribute(url)).append('"');

		xml.append(">\n\t\t").append(getPhase(job, false)).append("\n\t</jobref>");

		return xml.toString();
	}

	@Override
	public String getJobID(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<jobId").append(getUWSNamespace(root)).append('>').append(escapeXMLData(job.getJobId())).append("</jobId>").toString();
	}

	@Override
	public String getRunID(final UWSJob job, final boolean root){
		if (job.getRunId() != null){
			StringBuffer xml = new StringBuffer(root ? getHeader() : "");
			xml.append("<runId").append(getUWSNamespace(root));
			xml.append('>').append(escapeXMLData(job.getRunId())).append("</runId>");
			return xml.toString();
		}else
			return "";
	}

	@Override
	public String getOwnerID(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<ownerId").append(getUWSNamespace(root));
		if (job.getOwner() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append('>').append(escapeXMLData(job.getOwner().getPseudo())).append("</ownerId>");
		return xml.toString();
	}

	@Override
	public String getPhase(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<phase").append(getUWSNamespace(root)).append('>').append(job.getPhase()).append("</phase>").toString();
	}

	@Override
	public String getQuote(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<quote").append(getUWSNamespace(root));
		if (job.getQuote() <= 0)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append('>').append(job.getQuote()).append("</quote>");
		return xml.toString();
	}

	@Override
	public String getStartTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<startTime").append(getUWSNamespace(root));
		if (job.getStartTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append('>').append(ISO8601Format.format(job.getStartTime())).append("</startTime>");
		return xml.toString();
	}

	@Override
	public String getEndTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<endTime").append(getUWSNamespace(root));
		if (job.getEndTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append('>').append(ISO8601Format.format(job.getEndTime())).append("</endTime>");
		return xml.toString();
	}

	@Override
	public String getDestructionTime(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<destruction").append(getUWSNamespace(root));
		if (job.getDestructionTime() == null)
			xml.append(" xsi:nil=\"true\" />");
		else
			xml.append('>').append(ISO8601Format.format(job.getDestructionTime())).append("</destruction>");
		return xml.toString();
	}

	@Override
	public String getExecutionDuration(final UWSJob job, final boolean root){
		return (new StringBuffer(root ? getHeader() : "")).append("<executionDuration").append(getUWSNamespace(root)).append('>').append(job.getExecutionDuration()).append("</executionDuration>").toString();
	}

	@Override
	public String getErrorSummary(final ErrorSummary error, final boolean root){
		if (error != null){
			StringBuffer xml = new StringBuffer(root ? getHeader() : "");
			xml.append(tabPrefix).append("<errorSummary").append(getUWSNamespace(root));
			xml.append(" type=\"").append(error.getType()).append('"').append(" hasDetail=\"").append(error.hasDetail()).append("\">");
			xml.append("\n\t").append(tabPrefix).append("<message>").append(escapeXMLData(error.getMessage())).append("</message>");
			xml.append('\n').append(tabPrefix).append("</errorSummary>");
			return xml.toString();
		}else
			return "";
	}

	@Override
	public String getAdditionalParameters(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<parameters").append(getUWSNamespace(root)).append('>');
		String newLine = "\n\t" + tabPrefix;
		for(String paramName : job.getAdditionalParameters())
			xml.append(newLine).append(getAdditionalParameter(paramName, job.getAdditionalParameterValue(paramName), false));
		xml.append('\n').append(tabPrefix).append("</parameters>");
		return xml.toString();
	}

	@Override
	public String getAdditionalParameter(final String paramName, final Object paramValue, final boolean root){
		if (paramName != null && paramValue != null){
			// If ROOT, just the value must be returned:
			if (root){
				if (paramValue.getClass().isArray()){
					StringBuffer buf = new StringBuffer();
					for(Object o : (Object[])paramValue){
						if (buf.length() > 0)
							buf.append(';');
						buf.append(o.toString());
					}
					return buf.toString();
				}else
					return paramValue.toString();
			}
			// OTHERWISE, return the XML description:
			else{
				StringBuffer buf = new StringBuffer();
				// if array (=> multiple occurrences of the parameter), each item must be one individual parameter:
				if (paramValue.getClass().isArray()){
					for(Object o : (Object[])paramValue){
						if (buf.length() > 0)
							buf.append("\n\t").append(tabPrefix);
						buf.append(getAdditionalParameter(paramName, o, root));
					}
				}
				// otherwise, just return the XML parameter description:
				else{
					buf.append("<parameter").append(getUWSNamespace(root)).append(" id=\"").append(escapeXMLAttribute(paramName)).append('"');
					if (paramValue instanceof UploadFile)
						buf.append(" byReference=\"true\"");

					/* NOTE The attribute isPost is not supported in this library.
					 *      This information is not stored by the UWS Library and
					 *      so is never reported in the XML serialization of the job.
					 *      Besides, this attribute is optional. */

					buf.append('>').append(escapeXMLData(paramValue.toString())).append("</parameter>");
				}
				return buf.toString();
			}
		}
		// If NO VALUE or NO NAME, return an empty string:
		else
			return "";
	}

	@Override
	public String getResults(final UWSJob job, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<results").append(getUWSNamespace(root)).append('>');

		Iterator<Result> it = job.getResults();
		String newLine = "\n\t" + tabPrefix;
		while(it.hasNext())
			xml.append(newLine).append(getResult(it.next(), false));
		xml.append('\n').append(tabPrefix).append("</results>");
		return xml.toString();
	}

	@Override
	public String getResult(final Result result, final boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append("<result").append(getUWSNamespace(root)).append(" id=\"").append(escapeXMLAttribute(result.getId())).append('"');

		// The XLink attributes are optional. So if no URL is available, none will be written here:
		if (result.getHref() != null){
			xml.append(" xlink:type=\"").append((result.getType() == null) ? "simple" : escapeXMLAttribute(result.getType())).append('"');
			xml.append(" xlink:href=\"").append(escapeXMLAttribute(result.getHref())).append('"');
		}

		/* The attributes SIZE and MIME-TYPE are required from UWS 1.1.
		 * However, if no value is available, the following default values will be returned:
		 *     - If SIZE is negative of, 0 will be returned.
		 *     - If no MIME-TYPE is given, an empty string will be returned. */
		xml.append(" size=\"").append((result.getSize() < 0) ? 0 : result.getSize()).append('"');
		xml.append(" mime-type=\"").append((result.getMimeType() == null) ? "" : escapeXMLAttribute(result.getMimeType())).append('"');

		return xml.append(" />").toString();
	}

	@Override
	public String getJobInfo(UWSJob job, boolean root){
		StringBuffer xml = new StringBuffer(root ? getHeader() : "");
		xml.append(tabPrefix).append("<jobInfo").append(getUWSNamespace(root)).append('>');
		xml.append(job.getJobInfo().toXML(tabPrefix));
		xml.append("</jobInfo>");
		return xml.toString();
	}

	/**
	 * <p>Format the given value of piece of job information.</p>
	 * 
	 * <p>By default, this function is able to format the given types of object:</p>
	 * <ul>
	 * 	<li><b>Date</b>: formatted using the ISO-8601 format.</li>
	 * 	<li><b>Map.Entry&lt;?,?&gt;</b>: formated as a new XML child where the node name
	 * 	                                 will be the key and the node content will be the value.</li>
	 * 	<li><b>Array</b> or <b>Collection&lt;?&gt;</b>: each item is formatted using this function
	 * 	                                                and they are separated by a new line if they
	 * 	                                                are Map.Entry&lt;?,?&gt; or a ';' otherwise.</li>
	 * 	<li><b><i>Other</i></b>: the function toString() is merely called.</li>
	 * </ul>
	 * 
	 * @param value		The value to format.
	 * @param newLine	The string creating a new line.
	 * 
	 * @return			The formated value.
	 * 
	 * @since 4.2
	 */
	protected String formatPieceOfJobInfo(final Object value, String newLine){
		if (newLine == null)
			newLine = "\n\t";
		else
			newLine += "\t";

		// CASE: No value => ""
		if (value == null)
			return "";

		// CASE: Date => ISO-8601 format
		else if (value instanceof Date)
			return ISO8601Format.format((Date)value);

		// CASE MapEntry => <{key}>formatPieceOfJobInfo({value}, newLine)</{key}>
		else if (value instanceof Map.Entry<?,?>)
			return "<" + ((Map.Entry<?,?>)value).getKey() + ">" + formatPieceOfJobInfo(((Map.Entry<?,?>)value).getValue(), newLine) + "</" + ((Map.Entry<?,?>)value).getKey() + ">";

		// CASE: Array => one item per line if MapEntry, otherwise concatenation with ';'
		else if (value.getClass().isArray()){
			StringBuffer buf = new StringBuffer();
			for(Object o : (Object[])value){
				if (o == null)
					continue;
				else if (o instanceof Map.Entry<?,?>)
					buf.append(newLine);
				else if (buf.length() != 0)
					buf.append(';');
				buf.append(formatPieceOfJobInfo(o, newLine));
			}
			return buf.toString();
		}
		// CASE: Collection => one item per line if MapEntry, otherwise concatenation with ';'
		else if (value instanceof Collection<?>){
			StringBuffer buf = new StringBuffer();
			for(Object o : (Collection<?>)value){
				if (o == null)
					continue;
				else if (buf.length() != 0 && !(o instanceof Map.Entry<?,?>))
					buf.append(';');
				buf.append(formatPieceOfJobInfo(o, newLine));
			}
			return buf.toString();
		}
		// DEFAULT: toString()
		else
			return value.toString();
	}

	/* ************** */
	/* ESCAPE METHODS */
	/* ************** */
	/**
	 * Escapes the content of a node (data between the open and the close tags).
	 * 
	 * @param data	Data to escape.
	 * 
	 * @return		Escaped data.
	 */
	public static String escapeXMLData(final String data){
		StringBuffer encoded = new StringBuffer();
		for(int i = 0; i < data.length(); i++){
			char c = data.charAt(i);
			switch(c){
				case '&':
					encoded.append("&amp;");
					break;
				case '<':
					encoded.append("&lt;");
					break;
				case '>':
					encoded.append("&gt;");
					break;
				default:
					encoded.append(ensureLegalXml(c));
			}
		}
		return encoded.toString();
	}

	/**
	 * Escapes the given value of an XML attribute.
	 * 
	 * @param value	Value of an XML attribute.
	 * 
	 * @return		The escaped value.
	 */
	public static String escapeXMLAttribute(final String value){
		StringBuffer encoded = new StringBuffer();
		for(int i = 0; i < value.length(); i++){
			char c = value.charAt(i);
			switch(c){
				case '&':
					encoded.append("&amp;");
					break;
				case '<':
					encoded.append("&lt;");
					break;
				case '>':
					encoded.append("&gt;");
					break;
				case '"':
					encoded.append("&quot;");
					break;
				default:
					encoded.append(ensureLegalXml(c));
			}
		}
		return encoded.toString();
	}

	/**
	 * Escapes the given URL.
	 * 
	 * @param url	URL to escape.
	 * 
	 * @return		The escaped URL.
	 * 
	 * @see URLEncoder
	 * @see #escapeXMLAttribute(String)
	 */
	public static String escapeURL(final String url){
		try{
			return URLEncoder.encode(url, "UTF-8");
		}catch(UnsupportedEncodingException e){
			return escapeXMLAttribute(url);
		}
	}

	/**
	 * <p>Returns a legal XML character corresponding to an input character.
	 * Certain characters are simply illegal in XML (regardless of encoding).
	 * If the input character is legal in XML, it is returned;
	 * otherwise some other weird but legal character 
	 * (currently the inverted question mark, "\u00BF") is returned instead.</p>
	 * 
	 * <p><i>Note: copy of the STILTS VOSerializer.ensureLegalXml(char) function.</i></p>
	 *
	 * @param   c  input character
	 * @return  legal XML character, <code>c</code> if possible
	 * 
	 * @since 4.1
	 */
	public static char ensureLegalXml(char c){
		return ((c >= '\u0020' && c <= '\uD7FF') || (c >= '\uE000' && c <= '\uFFFD') || ((c) == 0x09 || (c) == 0x0A || (c) == 0x0D)) ? c : '\u00BF';
	}

}
