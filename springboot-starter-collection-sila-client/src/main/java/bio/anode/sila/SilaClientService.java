package bio.anode.sila;

import bio.anode.sila.connection.SilaChannel;
import bio.anode.sila.connection.SilaChannelManager;

import java.util.Map;
import java.util.Set;

public class SilaClientService {

    private final SilaChannelManager channelManager;

    public SilaClientService(SilaChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public SilaChannel getChannel(String serverName) {
        return channelManager.getChannel(serverName);
    }

    public Map<String, SilaChannel> getChannels() {
        return channelManager.getChannels();
    }

    public Set<String> getServerNames() {
        return channelManager.getChannels().keySet();
    }

    public SilaChannelManager getChannelManager() {
        return channelManager;
    }
}
