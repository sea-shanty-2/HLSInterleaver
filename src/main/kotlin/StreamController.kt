import io.github.rybalkinsd.kohttp.ext.httpGet
import io.javalin.Context
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object StreamController {
    private val streamMap: ConcurrentMap<String, ProxyStream> = ConcurrentHashMap()

    fun getStream(ctx: Context) {
        val id: String? = ctx.pathParam("stream-id")

        when (id) {
            null -> ctx.status(400)
            in streamMap -> {
                streamMap[id]?.let {
                    ctx.contentType("application/vnd.apple.mpegurl")
                    ctx.result(it.synthesize())
                }
            }
            else -> ctx.status(404)
        }
    }

    fun getSegment(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")
        val segmentId: String? = ctx.pathParam("segment-id")

        if (streamId == null || segmentId == null) {
            ctx.status(404)
        } else {
            when (streamId) {
                in streamMap -> {
                    val segment = streamMap[streamId]?.getSegmentURL(segmentId)
                    when (segment) {
                        null -> ctx.status(404)
                        else -> {
                            runBlocking {
                                Segments.retrieve(segment)?.let {
                                    ctx.result(ByteArrayInputStream(it))
                                    ctx.contentType("application/octet-stream")
                                } ?: run {
                                    ctx.status(404)
                                    println("Failed to retrieve segment $segment")
                                }
                            }
                        }
                    }
                }
                else -> ctx.status(404)
            }
        }
    }

    fun addStream(name: String, endpoints: Array<String>) {
        this.streamMap[name] = ProxyStream(name, endpoints)
    }
}
