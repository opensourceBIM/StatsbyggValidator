package org.bimserver.statsbyggvalidator;

import java.util.Date;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SExtendedData;
import org.bimserver.interfaces.objects.SExtendedDataSchema;
import org.bimserver.interfaces.objects.SFile;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.validationreport.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class StatsbyggValidatorService extends ServicePlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatsbyggValidatorService.class);
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
		final String schemaNamespace = "http://extend.bimserver.org/validationreport";
		serviceDescriptor.setWriteExtendedData(schemaNamespace);
		registerNewRevisionHandler(serviceDescriptor, new NewRevisionHandler() {
			@Override
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, long soid, SObjectType settings) throws ServerException, UserException {
				try {
					String title = "Running StatsbyggValidator";
					Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, title);
					SLongActionState state = new SLongActionState();
					Date startDate = new Date();
					state.setProgress(-1);
					state.setTitle(title);
					state.setState(SActionState.STARTED);
					state.setStart(startDate);
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					
					SExtendedDataSchema schema = bimServerClientInterface.getBimsie1ServiceInterface().getExtendedDataSchemaByNamespace(schemaNamespace);

					IfcModelInterface model = bimServerClientInterface.getModel(poid, roid, true);
					
					StandsbyggValidator standsbyggValidator = new StandsbyggValidator();
					ValidationReport validationReport = standsbyggValidator.validate(model);
					
					SFile file = new SFile();
					file.setMime("application/json; charset=utf-8");
					file.setFilename("validationresults.json");
					file.setData(validationReport.toJson().toString(2).getBytes(Charsets.UTF_8));
					file.setOid(bimServerClientInterface.getServiceInterface().uploadFile(file));
					
					SExtendedData extendedData = new SExtendedData();
					extendedData.setTitle("Statsbygg Validation Report");
					extendedData.setSchemaId(schema.getOid());
					extendedData.setFileId(file.getOid());
					
					bimServerClientInterface.getBimsie1ServiceInterface().addExtendedDataToRevision(roid, extendedData);

					state = new SLongActionState();
					state.setProgress(100);
					state.setTitle(title);
					state.setState(SActionState.FINISHED);
					state.setStart(startDate);
					state.setEnd(new Date());
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);

					bimServerClientInterface.getRegistry().unregisterProgressTopic(topicId);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			}
		});
	}
}