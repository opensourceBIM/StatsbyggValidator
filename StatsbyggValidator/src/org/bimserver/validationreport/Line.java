package org.bimserver.validationreport;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Line extends Item {
	private String fieldOrClass;
	private String value;
	private String shouldBe;
	private Type type;

	public Line(Type type, String fieldOrClass, String value, String shouldBe) {
		this.type = type;
		this.fieldOrClass = fieldOrClass;
		this.value = value;
		this.shouldBe = shouldBe;
	}
	
	public void toHtml(StringBuilder sb) {
		sb.append("<tr>");
		sb.append("<td>");
		sb.append(fieldOrClass);
		sb.append("</td>");
		sb.append("<td class=\"" + type.name().toLowerCase() + "\">");
		sb.append(value);
		sb.append("</td>");
		sb.append("<td>");
		sb.append(shouldBe);
		sb.append("</td>");
		sb.append("</tr>\n");
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject result = new JSONObject();
		result.put("type", "line");
		result.put("status", type.name());
		result.put("identification", fieldOrClass);
		result.put("value", value);
		result.put("shouldBe", shouldBe);
		return result;
	}
}