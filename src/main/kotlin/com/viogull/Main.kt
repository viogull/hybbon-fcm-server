package com.viogull

import com.diogonunes.jcdp.color.ColoredPrinter
import com.diogonunes.jcdp.color.api.Ansi
import com.typesafe.config.ConfigFactory
import net.tomp2p.connection.ConnectionBean
import net.tomp2p.nat.PeerBuilderNAT
import net.tomp2p.relay.RelayType
import net.tomp2p.relay.android.AndroidRelayServerConfig
import net.tomp2p.relay.buffer.MessageBufferConfiguration
import net.tomp2p.relay.tcp.TCPRelayServerConfig
import org.hive2hive.core.api.H2HNode
import org.hive2hive.core.api.configs.FileConfiguration
import org.hive2hive.core.api.configs.NetworkConfiguration
import org.hive2hive.core.network.H2HStorageMemory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException


class Main(port: Int, bootstrapEnabled: Boolean, bootstrapAddress: InetAddress, bootstrapPort: Int,
           externalAddress: InetAddress?, acceptData: Boolean, enableRelaying: Boolean, androidConfig: AndroidRelayServerConfig?) {
    internal var printer = ColoredPrinter.Builder(1, false).foreground(Ansi.FColor.BLUE).background(Ansi.BColor.WHITE).build()

    init {

        val node = H2HNode.createNode(fileComfig)
        val netConf = NetworkConfiguration.createInitial()
        printer.debugPrint("NODEID: " + netConf.nodeID)
        netConf.port = port

        if (bootstrapEnabled) {
            netConf.setBootstrap(bootstrapAddress)
            netConf.bootstrapPort = bootstrapPort
        }

        if (!node.connect(netConf)) {
            printer.errorPrint("Peer cannot connect!")

        } else if (node.connect(netConf)) {
            printer.debugPrint("Peer connecting...Adress:" + node.peer.peerAddress())
        }

        if (externalAddress != null) {
            printer.debugPrint("\nBinding to address {}" + externalAddress)
            val peerAddress = node.peer.peerBean().serverPeerAddress().changeAddress(externalAddress)
            node.peer.peerBean().serverPeerAddress(peerAddress)
        }

        if (!acceptData) {
            printer.debugPrint("Denying all data requests on this peer")
            val storageLayer = node.peer.storageLayer() as H2HStorageMemory
            storageLayer.setPutMode(H2HStorageMemory.StorageMemoryPutMode.DENY_ALL)
        }

        val storageMemory = node.peer.storageLayer() as H2HStorageMemory
        storageMemory.setPutMode(H2HStorageMemory.StorageMemoryPutMode.STANDARD)

        // start relaying if required
        if (enableRelaying) {
            printer.debugPrint("\n\n Starting relay functionality...")
            val nat = PeerBuilderNAT(node.peer.peer())
            val c = TCPRelayServerConfig()
            if (androidConfig != null) {
                nat.addRelayServerConfiguration(RelayType.ANDROID, androidConfig)
            }
            nat.start()

        }


    }

    companion object {


        private val fileComfig = FileConfiguration.createDefault()

        init {
            ConnectionBean.DEFAULT_CONNECTION_TIMEOUT_TCP = 20000
            ConnectionBean.DEFAULT_UDP_IDLE_MILLIS = 12000
            ConnectionBean.DEFAULT_TCP_IDLE_MILLIS = 12000
        }


        @Throws(UnknownHostException::class)
        @JvmStatic fun main(args: Array<String>) {
            val config = ConfigFactory.load("settings.conf")
            val port = config.getInt("port")
            val bootstrapEnabled = config.getBoolean("Bootstrap.enabled")
            val inetString = config.getString("Bootstrap.address")
            val bootstrapAddress = InetAddress.getByName(inetString)
            val bootstrapPort = config.getInt("Bootstrap.port")

            var externalAdress: InetAddress? = null
            val extAdrStr = config.getString("ExternalAddress")
            val adress_lcl = Inet4Address.getByName(inetString)

            if (!"auto".equals(extAdrStr, ignoreCase = true))
                externalAdress = InetAddress.getByName(extAdrStr)


            val acceptData = config.getBoolean("AcceptData")
            val enableRelaying = config.getBoolean("Relay.enabled")
            var androidServer: AndroidRelayServerConfig? = null
            if (enableRelaying) {
                val gcmKey = config.getString("Relay.GCM.api-key")
                val bufferTimeout = java.lang.Long.parseLong(config.getString("Relay.GCM.buffer-age-limit"))
                val buffer = MessageBufferConfiguration().bufferAgeLimit(bufferTimeout)
                androidServer = AndroidRelayServerConfig(gcmKey, 5, buffer)
            }

            Main(port, bootstrapEnabled, bootstrapAddress, bootstrapPort, externalAdress, acceptData,
                    enableRelaying, androidServer)

        }
    }
}