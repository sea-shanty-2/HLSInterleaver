import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import java.util.*

fun main() {
    val app = Javalin.create().server{
        Server().apply {
            connectors = arrayOf(ServerConnector(this).apply {
                this.host = "relay"
                this.port = 7000
            })
        }
    }.start()

    /* Sample relayed stream */
    StreamController.addStream("test", Arrays.asList("http://nginx/hls/test.m3u8"))

    /* Routes */
    app.routes {
        path("relay") {
            get(StreamController::getStreamList)
            path(":stream-id") {
                path("thumbnail") {
                    get(StreamController::getThumbnail)
                }
                get(StreamController::getStream)
                post(StreamController::createStream)
                delete(StreamController::deleteStream)
                path(":playlist-id") {
                    get(StreamController::getSubPlaylist)
                }
                path("segment/:segment-id") {
                    get(StreamController::getSegment)
                }
            }
        }
    }

    app.error(404) {
        println("Could not find: ${it.url()}")
    }
}