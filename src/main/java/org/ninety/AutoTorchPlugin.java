package org.ninety;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class AutoTorchPlugin extends Plugin {
	
	@Override
	public void onLoad() {
		final AutoTorchModule autoTorchModule = new AutoTorchModule();
		RusherHackAPI.getModuleManager().registerFeature(autoTorchModule);
	}
	
	@Override
	public void onUnload() {}
	
}