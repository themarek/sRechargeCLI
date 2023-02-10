import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import kotlinx.coroutines.runBlocking
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream

//https://github.com/Siumba/NewMotionAPI

data class Configuration(var chargerId:String="", var card:String="", var username:String="", var password:String="")
lateinit var Config: Configuration

class CmdMain:CliktCommand(){
    override fun run() = runBlocking {
        Config = Yaml().load(FileInputStream("config.yaml"))
        SRecharge.getSessionId(Config.username, Config.password)
    }
}
class StartLoading : CliktCommand(help = "Start loading",name ="start") {
    override fun run(): Unit = runBlocking{println(SRecharge.startLoading(Config.chargerId, Config.card))}
}
class StopLoading : CliktCommand(help = "Stop loading",name ="stop") {
    override fun run(): Unit = runBlocking{println(SRecharge.stopLoading(Config.chargerId))}
}
class Restart : CliktCommand(help = "Restart device",name ="restart") {
    override fun run(): Unit = runBlocking{println(SRecharge.resetStation(Config.chargerId))}
}
class Info : CliktCommand(help = "status of the device",name ="info") {
    override fun run(): Unit = runBlocking{println(SRecharge.getInformation())}
}

fun main(args: Array<String>) = CmdMain().subcommands(StartLoading(), StopLoading(), Restart(), Info()).main(args)