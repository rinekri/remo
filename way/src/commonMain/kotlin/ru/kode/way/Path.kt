package ru.kode.way

import kotlin.jvm.JvmInline

@JvmInline
value class Path(val segments: List<Segment>) {

  constructor(segment: Segment) : this(listOf(segment))
  constructor(
    segmentName: String,
    vararg segmentNames: String
  ) : this(listOf(segmentName, *segmentNames).map(::Segment))

  companion object;

  init {
    check(segments.isNotEmpty()) { "path must have at least one segment" }
    check(segments.all { it.name.isNotBlank() }) { "all path segments must be non-blank in path=$this" }
  }

  override fun toString(): String {
    return segments.joinToString(".") { it.name }
  }
}

@JvmInline
value class Segment(val name: String)

fun Path.tail(): Path {
  return Path(segments.drop(1))
}

fun Path.head(): Segment {
  return segments.first()
}

fun Path.drop(count: Int): Path {
  return Path(segments.drop(count))
}

fun Path.dropLast(count: Int): Path {
  return Path(segments.dropLast(count))
}

fun Path.take(count: Int): Path {
  return Path(segments.take(count))
}

fun Path.startsWith(other: Path): Boolean {
  return this.take(other.segments.size) == other
}

fun Path.prepend(path: Path): Path {
  return Path(path.segments + segments)
}

fun Path.append(path: Path): Path {
  return Path(this.segments + path.segments)
}

// app.permissions.intro → [app, app.permissions, app.permissions.intro]
fun Path.toSteps(): List<Path> {
  return (segments.indices).map { i -> this.take(i + 1) }
}
