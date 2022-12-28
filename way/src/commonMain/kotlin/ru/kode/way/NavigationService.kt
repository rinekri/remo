package ru.kode.way

// TODO add constructor for parallel regions: it would accept several roots
class NavigationService(private val schema: Schema, private val nodeBuilder: NodeBuilder) {
  private var state: NavigationState = NavigationState(_regions = mutableMapOf())
  private val listeners = ArrayList<(NavigationState) -> Unit>()

  fun start() {
    sendEvent(Event.Init)
  }

  fun addTransitionListener(listener: (NavigationState) -> Unit) {
    listeners.add(listener)
    if (state.isInitialized()) {
      listener(state)
    }
  }

  fun removeTransitionListener(listener: (NavigationState) -> Unit) {
    listeners.remove(listener)
  }

  private fun transition(state: NavigationState, event: Event): NavigationState {
    check(event == Event.Init || state.isInitialized()) {
      "internal error: no regions in state after Event.Init"
    }
    if (event == Event.Init) {
      schema.regions.forEach { regionId ->
        val regionRoot = nodeBuilder.build(regionId.path)
        require(regionRoot is FlowNode<*, *>) {
          "expected FlowNode at $regionId, but builder returned ${regionRoot::class.simpleName}"
        }
        state._regions[regionId] = Region(
          _nodes = mutableMapOf(regionId.path to regionRoot),
          _active = regionId.path,
          _alive = mutableListOf(regionId.path),
          _finishHandlers = mutableMapOf(),
        )
      }
    }
    println("on [$event] transition")
    val resolvedTransition = resolveTransition(schema, state.regions, nodeBuilder, event)
    println("=> resolved to ${resolvedTransition.targetPaths.values.first()}")
    return calculateAliveNodes(schema, state, resolvedTransition.targetPaths).also {
      storeFinishHandlers(it, resolvedTransition)
      synchronizeNodes(it)
      // TODO remove after ksp-generation impl, or run only in debug / during tests?
      checkSchemaValidity(schema, it)
    }
  }

  private fun checkSchemaValidity(schema: Schema, state: NavigationState) {
    state.regions.forEach { (regionId, region) ->
      region._nodes.forEach { (path, node) ->
        when (node) {
          is FlowNode<*, *> -> {
            check(schema.nodeType(regionId, path) == Schema.NodeType.Flow) {
              "according to schema, \"$path\" should be a flow node, but it is a \"${node::class.simpleName}\""
            }
          }
          is ScreenNode<*> -> {
            check(schema.nodeType(regionId, path) == Schema.NodeType.Screen) {
              "according to schema, \"$path\" should be a screen node, but it is a \"${node::class.simpleName}\""
            }
          }
        }
      }
    }
  }

  private fun storeFinishHandlers(state: NavigationState, resolvedTransition: ResolvedTransition) {
    if (resolvedTransition.finishHandlers == null) return
    resolvedTransition.finishHandlers.forEach { (regionId, handler) ->
      val region = state._regions[regionId] ?: error("no region with id \"$regionId\"")
      // the path to the flow finish handler of which we are storing
      region._finishHandlers[handler.flowPath] = handler.callback
    }
  }

  private fun synchronizeNodes(state: NavigationState) {
    state._regions.values.forEach { region ->
      region.alive.forEach { path ->
        region._nodes.keys.retainAll(region.alive.toSet())
        if (!region._nodes.containsKey(path)) {
          region._nodes[path] = nodeBuilder.build(path)
        }
      }
      region._finishHandlers.keys.retainAll(region.alive.toSet())
    }
  }

  fun sendEvent(event: Event) {
    state = transition(state, event)
    val validityErrors = state.runValidityChecks()
    if (validityErrors.isNotEmpty()) {
      error(validityErrors.joinToString("\n", prefix = "internal error. State is inconsistent:\n"))
    }
    listeners.forEach { it(state) }
  }
}

private fun NavigationState.runValidityChecks(): List<String> {
  return regions.mapNotNull { (regionId, region) ->
    if (region.alive.toSet() != region.nodes.keys) {
      "region \"$regionId\": alive node path set is different from nodes set. Alive paths: " +
        "${region.alive}, alive nodes: ${region.nodes.keys}"
    } else null
  }
}

private fun NavigationState.isInitialized(): Boolean {
  return this.regions.isNotEmpty()
}

// TODO be more sensible, actually calculate!
private fun Schema.regionCount() = 1
