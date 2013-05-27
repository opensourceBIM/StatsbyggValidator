package org.bimserver.statsbyggvalidator;


public class ValidationReport {

	public enum Type {
		ERROR,
		SUCCESS
	}
	
	private String header = "";
	private String footer = "";
	private StringBuilder data = new StringBuilder();

	public void addHeader(String value) {
		data.append("<tr><td colspan=\"3\" class=\"header\">" + value + "</td></tr>\n");
	}

	public void addError(String key, String value) {
		data.append("<tr><td>" + key + "</td><td class=\"error\" colspan=\"2\">" + value + "</td></tr>\n");
	}

	public void addSuccess(String key, String value) {
		data.append("<tr><td>" + key + "</td><td class=\"success\" colspan=\"2\">" + value + "</td></tr>\n");
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(header);
		sb.append(data);
		sb.append(footer);
		return sb.toString();
	}

	public void add(Type type, String key, String is, String shouldBe) {
		data.append("<tr>");
		data.append("<td>");
		data.append(key);
		data.append("</td>");
		data.append("<td class=\"" + type.name().toLowerCase() + "\">");
		data.append(is);
		data.append("</td>");
		data.append("<td>");
		data.append(shouldBe);
		data.append("</td>");
		data.append("</tr>\n");
	}
}