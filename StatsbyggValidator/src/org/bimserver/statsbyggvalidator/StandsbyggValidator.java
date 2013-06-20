package org.bimserver.statsbyggvalidator;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.IfcObject;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByType;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcSIUnitName;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcUnitAssignment;
import org.bimserver.validationreport.Type;
import org.bimserver.validationreport.ValidationReport;
import org.eclipse.emf.common.util.EList;

public class StandsbyggValidator {

	public ValidationReport validate(IfcModelInterface model) {
		ValidationReport validationReport = new ValidationReport();
		
		processUnits(model, validationReport);
		processIfcProject(model, validationReport);
		processIfcObjectType(model, validationReport);
		
		return validationReport;
	}

	/**
	 * BIM Manual Ref 29
	 * @param model
	 * @param validationReport
	 */
	private void processIfcObjectType(IfcModelInterface model, ValidationReport validationReport) {
		validationReport.addHeader("Object Types (29)");
		for (IfcObject ifcObject : model.getAllWithSubTypes(IfcObject.class)) {
			processIfcObjectType(validationReport, ifcObject);
		}
	}

	private void processIfcObjectType(ValidationReport validationReport, IfcObject ifcObject) {
		EList<IfcRelDefines> isDefinedBy = ifcObject.getIsDefinedBy();
		IfcRelDefinesByType ifcRelDefinesByType = null;
		for (IfcRelDefines ifcRelDefines : isDefinedBy) {
			if (ifcRelDefines instanceof IfcRelDefinesByType) {
				if (ifcRelDefinesByType != null) {
					validationReport.add(Type.ERROR, ifcObject.getOid(), getName(ifcObject), "too many IfcRelDefinesByType found", "");
					return;
				}
				ifcRelDefinesByType = (IfcRelDefinesByType)ifcRelDefines;
			}						
		}
		if (ifcRelDefinesByType != null) {
			if (ifcRelDefinesByType.getRelatingType() != null) {
				validationReport.add(Type.SUCCESS, ifcObject.getOid(), getName(ifcObject), "Valid", "");
			} else {
				validationReport.add(Type.ERROR, ifcObject.getOid(), getName(ifcObject), "no RelatingType found", "");
			}
		} else {
			validationReport.add(Type.ERROR, ifcObject.getOid(), getName(ifcObject), "no IfcRelDefinesByType found", "");
		}
	}

	private String getName(IfcObject ifcObject) {
		return ifcObject.eClass().getName() + " #" + ifcObject.getOid() + (ifcObject.getName() == null || ifcObject.getName().trim().equals("") ? "" : " (" + ifcObject.getName() + ")");
	}

	/**
	 * BIM Manual Ref 11
	 * @param model
	 * @param validationReport
	 */
	private void processIfcProject(IfcModelInterface model, ValidationReport validationReport) {
		validationReport.addHeader("Project (11)");
		for (IfcProject ifcProject : model.getAll(IfcProject.class)) {
			if (ifcProject.getName().trim().length() == 5) {
				try {
					Integer.parseInt(ifcProject.getName().trim());
					validationReport.add(Type.SUCCESS, ifcProject.getOid(), "Project Name", ifcProject.getName(), "Project name should be 5 digits");
					return;
				} catch (NumberFormatException e) {
				}
			}
			validationReport.add(Type.ERROR, ifcProject.getOid(), "Project Name", ifcProject.getName(), "Project name should be 5 digits");
			return;
		}
		validationReport.add(Type.ERROR, -1, "Project Name", "No Project", "Project name should be 5 digits, no project found");
	}

	/**
	 * BIM Manual Ref 9
	 * @param model
	 * @param builder
	 */
	private void processUnits(IfcModelInterface model, ValidationReport validationReport) {
		validationReport.addHeader("Units (9)");
		for (IfcProject ifcProject : model.getAll(IfcProject.class)) {
			IfcUnitAssignment ifcUnitAssignment = ifcProject.getUnitsInContext();
			for (IfcUnit ifcUnit : ifcUnitAssignment.getUnits()) {
				if (ifcUnit instanceof IfcSIUnit) {
					IfcSIUnit ifcSIUnit = (IfcSIUnit)ifcUnit;
					switch (ifcSIUnit.getUnitType()) {
					case LENGTHUNIT:
						if (ifcSIUnit.getPrefix() != IfcSIPrefix.MILLI || ifcSIUnit.getName() != IfcSIUnitName.METRE) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Length Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.MILLI.name() + " " + IfcSIUnitName.METRE.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Length Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.MILLI.name() + " " + IfcSIUnitName.METRE.name());
						}
						break;
					case AREAUNIT:
						if (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL || ifcSIUnit.getName() != IfcSIUnitName.SQUARE_METRE) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Area Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.SQUARE_METRE.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Area Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.SQUARE_METRE.name());
						}
						break;
					case VOLUMEUNIT:
						if (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL || ifcSIUnit.getName() != IfcSIUnitName.CUBIC_METRE) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Volume Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.CUBIC_METRE.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Volume Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.CUBIC_METRE.name());
						}
						break;
					case SOLIDANGLEUNIT:
						if (ifcSIUnit.getName() != IfcSIUnitName.RADIAN) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Solid Angle Unit", ifcSIUnit.getName().name(), IfcSIUnitName.RADIAN.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Solid Angle Unit", ifcSIUnit.getName().name(), IfcSIUnitName.RADIAN.name());
						}
						break;
					case MASSUNIT:
						if (ifcSIUnit.getPrefix() != IfcSIPrefix.KILO || ifcSIUnit.getName() != IfcSIUnitName.GRAM) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Mass Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.KILO.name() + " " + IfcSIUnitName.GRAM.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Mass Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : ""), IfcSIPrefix.KILO.name() + " " + IfcSIUnitName.GRAM.name());
						}
						break;
					case TIMEUNIT:
						if (ifcSIUnit.getName() != IfcSIUnitName.SECOND) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Time Unit", ifcSIUnit.getName().name(), IfcSIUnitName.SECOND.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Time Unit", ifcSIUnit.getName().name(), IfcSIUnitName.SECOND.name());
						}
						break;
					case LUMINOUSFLUXUNIT:
						if (ifcSIUnit.getName() != IfcSIUnitName.LUMEN) {
							validationReport.add(Type.ERROR, ifcSIUnit.getOid(), "Luminous Flux Unit", ifcSIUnit.getName().name(), IfcSIUnitName.LUMEN.name());
						} else {
							validationReport.add(Type.SUCCESS, ifcSIUnit.getOid(), "Luminous Flux Unit", ifcSIUnit.getName().name(), IfcSIUnitName.LUMEN.name());
						}
						break;
					// TODO Angle and Temperature seem not to be in IFC
					default:
					}
				}
			}
		}
	}
}
