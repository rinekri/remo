package ru.kode.test.app.schema

import kotlin.collections.List
import kotlin.collections.Set
import ru.kode.way.Path
import ru.kode.way.RegionId
import ru.kode.way.Schema
import ru.kode.way.Segment
import ru.kode.way.append

public class TestAppSchema(
  private val appSchema: Schema,
  private val loginSchema: Schema,
  private val mainSchema: Schema,
) : Schema {
  public override val regions: List<RegionId> = listOf(RegionId(Path("app")))

  public override fun children(regionId: RegionId): Set<Segment> = emptySet()

  public override fun children(regionId: RegionId, segment: Segment): Set<Segment> = emptySet()

  public override fun target(regionId: RegionId, segment: Segment): Path = when (regionId) {
    regions[0] -> {
      when(segment.name) {
        "app" -> Path("app").append(appSchema.target(regionId, segment))
        "login" -> Path("app","login").append(loginSchema.target(regionId, segment))
        "main" -> Path("app","login","main").append(mainSchema.target(regionId, segment))
        else -> error("""unknown segment=$segment""")
      }
    }
    else -> {
      error("""unknown regionId=$regionId""")
    }
  }

  public override fun nodeType(regionId: RegionId, path: Path): Schema.NodeType = when (regionId) {
    regions[0] -> {
      when {
        path.startsWith(Path("app","login","main")) -> Schema.NodeType.Flow
        path.startsWith(Path("app","login")) -> Schema.NodeType.Flow
        path.startsWith(Path("app")) -> Schema.NodeType.Flow
        else -> {
          error("""internal error: no nodeType for path=$path""")
        }
      }
    }
    else -> {
      error("""unknown regionId=$regionId""")
    }
  }
}
