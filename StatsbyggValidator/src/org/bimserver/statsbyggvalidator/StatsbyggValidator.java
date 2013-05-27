package org.bimserver.statsbyggvalidator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SExtendedData;
import org.bimserver.interfaces.objects.SFile;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.models.ifc2x3tc1.IfcObject;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByType;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcSIUnitName;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcUnitAssignment;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.services.BimServerClientException;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.statsbyggvalidator.ValidationReport.Type;
import org.eclipse.emf.common.util.EList;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class StatsbyggValidator extends ServicePlugin {

	private boolean initialized;

	@Override
	public void init(PluginManager pluginManager) throws PluginException {
		super.init(pluginManager);
		initialized = true;
	}
	
	@Override
	public String getDescription() {
		return "StatsbyggValidator";
	}

	@Override
	public String getDefaultName() {
		return "StatsbyggValidator";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public String getTitle() {
		return "StatsbyggValidator";
	}

	@Override
	public void register(PluginConfiguration pluginConfiguration) {
		ServiceDescriptor serviceDescriptor = StoreFactory.eINSTANCE.createServiceDescriptor();
		serviceDescriptor.setProviderName("BIMserver");
		serviceDescriptor.setIdentifier(getClass().getName());
		serviceDescriptor.setName("StatsbyggValidator");
		serviceDescriptor.setDescription("StatsbyggValidator");
		serviceDescriptor.setNotificationProtocol(AccessMethod.INTERNAL);
		serviceDescriptor.setTrigger(Trigger.NEW_REVISION);
		serviceDescriptor.setReadRevision(true);
		serviceDescriptor.setWriteExtendedData("http://extend.bimserver.org/validationreport");
		registerNewRevisionHandler(serviceDescriptor, new NewRevisionHandler() {
			@Override
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, SObjectType settings) throws ServerException, UserException {
				try {
					Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, "Running StatsbyggValidator");
					SLongActionState state = new SLongActionState();
					Date startDate = new Date();
					state.setProgress(-1);
					state.setTitle("Bezig...");
					state.setState(SActionState.FINISHED);
					state.setStart(startDate);
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					
					IfcModelInterface model = bimServerClientInterface.getModel(poid, roid, true);
					
					ValidationReport validationReport = new ValidationReport();
					InputStream resourceAsInputStream = getPluginManager().getPluginContext(StatsbyggValidator.this).getResourceAsInputStream("templates/header.html");
					validationReport.setHeader(CharStreams.toString(new InputStreamReader(resourceAsInputStream)));
					resourceAsInputStream.close();
					InputStream resourceAsInputStream2 = getPluginManager().getPluginContext(StatsbyggValidator.this).getResourceAsInputStream("templates/footer.html");
					validationReport.setFooter(CharStreams.toString(new InputStreamReader(resourceAsInputStream2)));
					resourceAsInputStream2.close();
					
					processUnits(model, validationReport);
					processIfcProject(model, validationReport);
					processIfcObjectType(model, validationReport);
					
					SFile file = new SFile();
					file.setMime("text/html; charset=utf-8");
					file.setFilename("StatsbyggValidator.html");
					file.setData(validationReport.toString().getBytes(Charsets.UTF_8));
					
					file.setOid(bimServerClientInterface.getServiceInterface().uploadFile(file));
					
					SExtendedData extendedData = new SExtendedData();
					extendedData.setTitle("StatsbyggValidator");
					extendedData.setFileId(file.getOid());
					
					bimServerClientInterface.getServiceInterface().addExtendedDataToRevision(roid, extendedData);
					
					bimServerClientInterface.getRegistry().unregisterProgressTopic(topicId);
				} catch (PublicInterfaceNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (BimServerClientException e) {
					e.printStackTrace();
				}
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
							validationReport.addError(getName(ifcObject), "too many IfcRelDefinesByType found");
							return;
						}
						ifcRelDefinesByType = (IfcRelDefinesByType)ifcRelDefines;
					}						
				}
				if (ifcRelDefinesByType != null) {
					if (ifcRelDefinesByType.getRelatingType() != null) {
						validationReport.addSuccess(getName(ifcObject), "Valid");
					} else {
						validationReport.addError(getName(ifcObject), "no RelatingType found");
					}
				} else {
					validationReport.addError(getName(ifcObject), "no IfcRelDefinesByType found");
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
							validationReport.add(Type.SUCCESS, "Project Name", ifcProject.getName(), "Project name should be 5 digits");
							return;
						} catch (NumberFormatException e) {
						}
					}
					validationReport.add(Type.ERROR, "Project Name", ifcProject.getName(), "Project name should be 5 digits");
					return;
				}
				validationReport.add(Type.ERROR, "Project Name", "", "Project name should be 5 digits, no project found");
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
									validationReport.add(Type.ERROR, "Length Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.MILLI.name() + " " + IfcSIUnitName.METRE.name());
								} else {
									validationReport.add(Type.SUCCESS, "Length Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.MILLI.name() + " " + IfcSIUnitName.METRE.name());
								}
								break;
							case AREAUNIT:
								if (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL || ifcSIUnit.getName() != IfcSIUnitName.SQUARE_METRE) {
									validationReport.add(Type.ERROR, "Area Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.SQUARE_METRE.name());
								} else {
									validationReport.add(Type.SUCCESS, "Area Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.SQUARE_METRE.name());
								}
								break;
							case VOLUMEUNIT:
								if (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL || ifcSIUnit.getName() != IfcSIUnitName.CUBIC_METRE) {
									validationReport.add(Type.ERROR, "Volume Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.CUBIC_METRE.name());
								} else {
									validationReport.add(Type.SUCCESS, "Volume Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIUnitName.CUBIC_METRE.name());
								}
								break;
							case SOLIDANGLEUNIT:
								if (ifcSIUnit.getName() != IfcSIUnitName.RADIAN) {
									validationReport.add(Type.ERROR, "Solid Angle Unit", ifcSIUnit.getName().name(), IfcSIUnitName.RADIAN.name());
								} else {
									validationReport.add(Type.SUCCESS, "Solid Angle Unit", ifcSIUnit.getName().name(), IfcSIUnitName.RADIAN.name());
								}
								break;
							case MASSUNIT:
								if (ifcSIUnit.getPrefix() != IfcSIPrefix.KILO || ifcSIUnit.getName() != IfcSIUnitName.GRAM) {
									validationReport.add(Type.ERROR, "Mass Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : "") + ifcSIUnit.getName().name(), IfcSIPrefix.KILO.name() + " " + IfcSIUnitName.GRAM.name());
								} else {
									validationReport.add(Type.SUCCESS, "Mass Unit", (ifcSIUnit.getPrefix() != IfcSIPrefix.NULL ? ifcSIUnit.getPrefix().name() + " " : ""), IfcSIPrefix.KILO.name() + " " + IfcSIUnitName.GRAM.name());
								}
								break;
							case TIMEUNIT:
								if (ifcSIUnit.getName() != IfcSIUnitName.SECOND) {
									validationReport.add(Type.ERROR, "Time Unit", ifcSIUnit.getName().name(), IfcSIUnitName.SECOND.name());
								} else {
									validationReport.add(Type.SUCCESS, "Time Unit", ifcSIUnit.getName().name(), IfcSIUnitName.SECOND.name());
								}
								break;
							case LUMINOUSFLUXUNIT:
								if (ifcSIUnit.getName() != IfcSIUnitName.LUMEN) {
									validationReport.add(Type.ERROR, "Luminous Flux Unit", ifcSIUnit.getName().name(), IfcSIUnitName.LUMEN.name());
								} else {
									validationReport.add(Type.SUCCESS, "Luminous Flux Unit", ifcSIUnit.getName().name(), IfcSIUnitName.LUMEN.name());
								}
								break;
							// TODO Angle and Temperature seem not to be in IFC
							default:
							}
						}
					}
				}
			}
		});
	}
}