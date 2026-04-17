package com.example.tsumap


import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.random.Random


enum class ItemTag {
    DISPOSABLE,
    RAMEN, WOK, RICE,
    SHAWARMA,
    BAKERY, DRINKS, SNACKS,
    COFFEE,
    COMBO_LUNCH,
    PANCAKES,
    FAST_FOOD
}

sealed class VenueHours {
    data object Open24h : VenueHours()
    data class Range(val open: LocalTime, val close: LocalTime) : VenueHours()
}

data class FoodPlace(
    val name: String,
    val point: Point,
    val offers: Set<ItemTag>,
    val hours: VenueHours
)

data class RouteConfig(
    val speedKmh: Double = 5.0,
    val kmPerGridUnit: Double,
    val serviceMinute: Double = 3.0,
    val penaltyUncovered: Double = 1e6,
    val penaltyClosed: Double = 1e6
)

data class RouteEval(
    val cost: Double,
    val totalMinutes: Double,
    val uncovered: Set<ItemTag>,
    val closedVisits: Int,
    val visitedOrder: List<Int>
)

data class GeneticRouteResult(
    val eval: RouteEval,
    val path: List<Pair<Int, Int>>,
    val stops: List<Point>
)

data class StitchedPathResult(
    val path: List<Pair<Int, Int>>,
    val reachedStops: List<Point>
)

typealias Chromosome = IntArray


fun foodVenuesCatalog(): List<FoodPlace> = listOf(
    FoodPlace(
        name = "Мария-Ра",
        point = Point(15, 4),
        offers = setOf(ItemTag.DISPOSABLE),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Цзисяни",
        point = Point(101, 6),
        offers = setOf(ItemTag.RAMEN, ItemTag.WOK, ItemTag.RICE),
        hours = VenueHours.Range(LocalTime.of(10, 0), LocalTime.of(19, 0))
    ),
    FoodPlace(
        name = "Безумно",
        point = Point(149, 9),
        offers = setOf(ItemTag.SHAWARMA),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Абрикос",
        point = Point(149, 15),
        offers = setOf(ItemTag.DISPOSABLE, ItemTag.BAKERY, ItemTag.DRINKS, ItemTag.SNACKS),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Пилад",
        point = Point(160, 33),
        offers = setOf(ItemTag.DRINKS, ItemTag.SNACKS),
        hours = VenueHours.Open24h
    ),
    FoodPlace(
        name = "XO Bakery",
        point = Point(92, 54),
        offers = setOf(ItemTag.COFFEE, ItemTag.BAKERY),
        hours = VenueHours.Range(LocalTime.of(10, 0), LocalTime.of(19, 0))
    ),
    FoodPlace(
        name = "Сыр-Бор",
        point = Point(135, 60),
        offers = setOf(ItemTag.COMBO_LUNCH),
        hours = VenueHours.Range(LocalTime.of(10, 0), LocalTime.of(15, 0))
    ),
    FoodPlace(
        name = "Сибирские блины (ЦК)",
        point = Point(104, 64),
        offers = setOf(ItemTag.PANCAKES),
        hours = VenueHours.Range(LocalTime.of(9, 0), LocalTime.of(20, 0))
    ),
    FoodPlace(
        name = "Укромное местечко",
        point = Point(167, 89),
        offers = setOf(ItemTag.COMBO_LUNCH),
        hours = VenueHours.Range(LocalTime.of(10, 0), LocalTime.of(17, 0))
    ),
    FoodPlace(
        name = "Научка",
        point = Point(76, 97),
        offers = setOf(ItemTag.COFFEE),
        hours = VenueHours.Range(LocalTime.of(8, 0), LocalTime.of(20, 0))
    ),
    FoodPlace(
        name = "Сибирские блины (Ленина)",
        point = Point(172, 116),
        offers = setOf(ItemTag.PANCAKES),
        hours = VenueHours.Range(LocalTime.of(9, 0), LocalTime.of(20, 0))
    ),
    FoodPlace(
        name = "Rostiks",
        point = Point(102, 120),
        offers = setOf(ItemTag.FAST_FOOD),
        hours = VenueHours.Range(LocalTime.of(8, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Гербарий",
        point = Point(122, 122),
        offers = setOf(ItemTag.COMBO_LUNCH),
        hours = VenueHours.Range(LocalTime.of(8, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Пятерочка",
        point = Point(4, 124),
        offers = setOf(ItemTag.DISPOSABLE, ItemTag.BAKERY, ItemTag.DRINKS, ItemTag.SNACKS),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Ближе",
        point = Point(150, 131),
        offers = setOf(ItemTag.COMBO_LUNCH),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Бристоль",
        point = Point(92, 137),
        offers = setOf(ItemTag.DISPOSABLE, ItemTag.DRINKS, ItemTag.SNACKS),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    ),
    FoodPlace(
        name = "Ярче",
        point = Point(148, 144),
        offers = setOf(ItemTag.DISPOSABLE, ItemTag.BAKERY, ItemTag.DRINKS, ItemTag.SNACKS),
        hours = VenueHours.Range(LocalTime.of(7, 0), LocalTime.of(23, 0))
    )
)


fun candidatesFor(need: Set<ItemTag>, catalog: List<FoodPlace>): List<FoodPlace> =
    catalog.filter { place -> place.offers.any { it in need } }

fun gridEuclideanDistance(a: Point, b: Point): Double =
    hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())

fun pathTravelMinutes(pathLengthUnits: Double, config: RouteConfig): Double {
    val km = pathLengthUnits * config.kmPerGridUnit
    val hours = km / config.speedKmh
    return hours * 60.0
}

fun canVisitAt(hours: VenueHours, time: LocalDateTime): Boolean {
    val cur = time.toLocalTime()
    return when (hours) {
        is VenueHours.Open24h -> true
        is VenueHours.Range -> {

            if (hours.open < hours.close) {

                cur >= hours.open && cur < hours.close
            } else {
                cur >= hours.open || cur < hours.close
            }
        }

        else -> true
    }
}


fun evaluateRoute(
    startPoint: Point,
    startTime: LocalDateTime,
    need: Set<ItemTag>,
    venues: List<FoodPlace>,
    order: Chromosome,
    config: RouteConfig
): RouteEval {
    val remaining = need.toMutableSet()
    var t = startTime
    var curPos = startPoint
    var travelMin = 0.0
    var serviceMin = 0.0
    var closedVisits = 0
    val visited = mutableListOf<Int>()
    var penalty = 0.0


    for (gene in order) {
        if (remaining.isEmpty()) break

        val place = venues[gene]

        val usefulOffers = place.offers.intersect(remaining).toSet()
        if (usefulOffers.isEmpty()) {
            continue
        }

        val legUnits = gridEuclideanDistance(curPos, place.point)
        val legMin = pathTravelMinutes(legUnits, config)
        val expectedArrivalTime = t.plusMinutes(legMin.roundToLong())


        if (!canVisitAt(place.hours, expectedArrivalTime)) {
            closedVisits++
            penalty += config.penaltyClosed
            continue
        }

        travelMin += legMin
        t = expectedArrivalTime

        serviceMin += config.serviceMinute
        t = t.plusMinutes(config.serviceMinute.roundToLong())

        remaining.removeAll(usefulOffers)
        curPos = place.point
        visited.add(gene)
    }

    if (remaining.isNotEmpty()) {
        penalty += remaining.size * config.penaltyUncovered
    }

    val totalMinutes = travelMin + serviceMin
    val cost = (totalMinutes + penalty).coerceAtLeast(0.0)

    return RouteEval(cost, totalMinutes, remaining.toSet(), closedVisits, visited)
}

fun randomPermutation(n: Int, rng: Random = Random.Default): Chromosome {
    val a = IntArray(n) { it }
    for (i in n - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = a[i]
        a[i] = a[j]
        a[j] = tmp
    }
    return a
}


fun tournamentPick(costs: DoubleArray, k: Int, rng: Random = Random.Default): Int {
    var best = rng.nextInt(costs.size)
    var bestCost = costs[best]
    repeat(k - 1) {
        val cand = rng.nextInt(costs.size)
        val c = costs[cand]
        if (c < bestCost) {
            best = cand
            bestCost = c
        }
    }
    return best
}


fun oxCrossover(a: Chromosome, b: Chromosome, rng: Random = Random.Default): Chromosome {
    require(a.size == b.size)
    val n = a.size
    if (n < 2) return a.copyOf()

    var i = rng.nextInt(n)
    var j = rng.nextInt(n)
    if (i > j) {
        val tmp = i
        i = j
        j = tmp
    }

    val child = IntArray(n) { -1 }
    for (k in i..j) child[k] = a[k]

    val middle = a.sliceArray(i..j).toSet()
    var write = (j + 1) % n
    for (x in b) {
        if (x in middle) continue
        child[write] = x
        write = (write + 1) % n
    }
    return child
}


fun mutateSwap(ch: Chromosome, rng: Random = Random.Default) {
    if (ch.size < 2) return
    val i = rng.nextInt(ch.size)
    var j = rng.nextInt(ch.size)
    while (j == i) j = rng.nextInt(ch.size)
    val t = ch[i]
    ch[i] = ch[j]
    ch[j] = t
}


data class GaParams(
    val populationSize: Int = 120,
    val maxGenerations: Int = 500,
    val crossoverRate: Double = 0.9,
    val mutationRate: Double = 0.12,
    val tournamentK: Int = 4,
    val elitism: Int = 2,
    val immigrants: Int = 2
)

fun runGeneticAlgorithm(
    startPoint: Point,
    startTime: LocalDateTime,
    need: Set<ItemTag>,
    venues: List<FoodPlace>,
    config: RouteConfig,
    params: GaParams = GaParams(),
    rng: Random = Random.Default,
    onGeneration: (generation: Int, best: RouteEval, bestChromosome: Chromosome) -> Unit
): Pair<RouteEval, Chromosome> {
    val n = venues.size
    require(n >= 1) { "Нужен хотя бы один кандидат" }
    val population = Array(params.populationSize) { randomPermutation(n, rng) }
    val costs = DoubleArray(params.populationSize)
    fun evalAll() {
        for (i in population.indices) {
            costs[i] =
                evaluateRoute(startPoint, startTime, need, venues, population[i], config).cost
        }
    }
    evalAll()
    val bestEverIdx = costs.indices.minBy { costs[it] }
    var bestEverChrom = population[bestEverIdx].copyOf()
    var bestEverEval = evaluateRoute(startPoint, startTime, need, venues, bestEverChrom, config)
    for (gen in 0 until params.maxGenerations) {
        evalAll()
        val bestIdx = costs.indices.minBy { costs[it] }
        val bestThisGen =
            evaluateRoute(startPoint, startTime, need, venues, population[bestIdx], config)
        onGeneration(gen, bestThisGen, population[bestIdx].copyOf())
        if (costs[bestIdx] < bestEverEval.cost) {
            bestEverChrom = population[bestIdx].copyOf()
            bestEverEval = evaluateRoute(startPoint, startTime, need, venues, bestEverChrom, config)
        }
        val sorted = costs.indices.sortedBy { costs[it] }
        val next = ArrayList<Chromosome>(params.populationSize)
        val elite = params.elitism.coerceAtMost(params.populationSize)
        for (e in 0 until elite) {
            next.add(population[sorted[e]].copyOf())
        }
        while (next.size < params.populationSize) {
            val p1 = population[tournamentPick(costs, params.tournamentK, rng)].copyOf()
            val p2 = population[tournamentPick(costs, params.tournamentK, rng)].copyOf()
            val child = if (rng.nextDouble() < params.crossoverRate) oxCrossover(
                p1,
                p2,
                rng
            ) else p1.copyOf()
            if (rng.nextDouble() < params.mutationRate) mutateSwap(child, rng)
            next.add(child)
        }
        val imm = params.immigrants.coerceAtMost(next.size)
        for (i in 0 until imm) {
            next[next.size - 1 - i] = randomPermutation(n, rng)
        }
        for (i in population.indices) {
            population[i] = next[i]
        }
    }
    evalAll()
    val finalIdx = costs.indices.minBy { costs[it] }
    if (costs[finalIdx] < bestEverEval.cost) {
        bestEverChrom = population[finalIdx].copyOf()
        bestEverEval = evaluateRoute(startPoint, startTime, need, venues, bestEverChrom, config)
    }
    return bestEverEval to bestEverChrom
}

fun buildGeneticPathOnGrid(
    grid: Array<IntArray>,
    start: Point,
    allCatalog: List<FoodPlace>,
    need: Set<ItemTag>,
    selectedCategoryKeys: List<String> = emptyList(),
    now: LocalDateTime = LocalDateTime.now(),
    kmPerGridUnit: Double = 0.01
): GeneticRouteResult {
    val empty = GeneticRouteResult(
        eval = RouteEval(1e9, 0.0, need, 0, emptyList()),
        path = emptyList(),
        stops = emptyList()
    )
    val snappedStartPair = findNearestRoad(grid, start.x, start.y)
    val snappedStart = if (snappedStartPair != null) {
        Point(snappedStartPair.first, snappedStartPair.second)
    } else {
        start
    }

    val snappedCatalog = allCatalog.mapNotNull { place ->
        val s = findNearestRoad(grid, place.point.x, place.point.y)
        if (s != null) place.copy(point = Point(s.first, s.second)) else null
    }
    if (snappedCatalog.isEmpty()) return empty

    val pickedVenues = if (selectedCategoryKeys.isNotEmpty()) {
        pickOneVenuePerCategory(selectedCategoryKeys, snappedCatalog, snappedStart, now)
    } else {
        candidatesFor(need, snappedCatalog)
    }
    if (pickedVenues.isEmpty()) return empty

    val config = RouteConfig(kmPerGridUnit = kmPerGridUnit)
    val orderedVenues = nearestNeighborOrder(snappedStart, pickedVenues)
    val orderedStops = orderedVenues.map { it.point }
    val stitched = stitchAstarPath(grid, snappedStart, orderedStops)

    var totalDist = 0.0
    var prev = snappedStart
    for (venue in orderedVenues) {
        totalDist += gridEuclideanDistance(prev, venue.point)
        prev = venue.point
    }
    val routeEval = RouteEval(
        cost = totalDist,
        totalMinutes = pathTravelMinutes(totalDist, config),
        uncovered = emptySet(),
        closedVisits = 0,
        visitedOrder = orderedVenues.indices.toList()
    )

    return GeneticRouteResult(
        eval = routeEval,
        path = stitched.path,
        stops = stitched.reachedStops
    )
}

fun stitchAstarPath(
    grid: Array<IntArray>,
    start: Point,
    targets: List<Point>
): StitchedPathResult {
    if (targets.isEmpty()) return StitchedPathResult(path = emptyList(), reachedStops = emptyList())

    val full = mutableListOf<Pair<Int, Int>>()
    val reached = mutableListOf<Point>()
    var cur = start
    var hasAnySegment = false

    for (p in targets) {
        if (cur.x == p.x && cur.y == p.y) {
            if (full.isEmpty()) {
                full.add(cur.x to cur.y)
                hasAnySegment = true
            }
            reached.add(p)
            continue
        }
        val seg = aStar(grid, cur.x to cur.y, p.x to p.y)
        if (seg.isNotEmpty()) {
            hasAnySegment = true
            if (full.isEmpty()) full.addAll(seg) else full.addAll(seg.drop(1))
            reached.add(p)
            cur = p
        }
    }

    return if (hasAnySegment) {
        StitchedPathResult(path = full, reachedStops = reached)
    } else {
        StitchedPathResult(path = emptyList(), reachedStops = emptyList())
    }
}

val CATEGORY_KEYS = listOf(
    "Одноразка", "Рамен/Вок/Рис", "Шаурма", "Выпечка",
    "Напитки", "Снеки", "Кофе", "Комплексный обед", "Блины", "Фастфуд"
)

fun categoryToTags(key: String): Set<ItemTag> =
    when (key.trim()) {
        "Одноразка" -> setOf(ItemTag.DISPOSABLE)
        "Рамен/Вок/Рис" -> setOf(ItemTag.RAMEN, ItemTag.WOK, ItemTag.RICE)
        "Шаурма" -> setOf(ItemTag.SHAWARMA)
        "Выпечка" -> setOf(ItemTag.BAKERY)
        "Напитки" -> setOf(ItemTag.DRINKS)
        "Снеки" -> setOf(ItemTag.SNACKS)
        "Кофе" -> setOf(ItemTag.COFFEE)
        "Комплексный обед" -> setOf(ItemTag.COMBO_LUNCH)
        "Блины" -> setOf(ItemTag.PANCAKES)
        "Фастфуд" -> setOf(ItemTag.FAST_FOOD)
        else -> emptySet()
    }

fun itemTagsToNeed(selected: Set<String>): Set<ItemTag> =
    selected.flatMapTo(mutableSetOf()) { categoryToTags(it) }

fun pickOneVenuePerCategory(
    categoryKeys: List<String>,
    snappedCatalog: List<FoodPlace>,
    startPoint: Point,
    now: LocalDateTime
): List<FoodPlace> {
    val usedIndices = mutableSetOf<Int>()
    val result = mutableListOf<FoodPlace>()
    for (key in categoryKeys) {
        val catTags = categoryToTags(key)
        if (catTags.isEmpty()) continue
        val matching = snappedCatalog.withIndex().filter { (idx, place) ->
            idx !in usedIndices && place.offers.any { it in catTags }
        }
        if (matching.isEmpty()) continue

        val pool = matching.filter { (_, place) -> canVisitAt(place.hours, now) }
            .ifEmpty { matching }
        val best = pool.minByOrNull { (_, place) -> gridEuclideanDistance(startPoint, place.point) }
        if (best != null) {
            result.add(best.value)
            usedIndices.add(best.index)
        }
    }

    return result
}

fun nearestNeighborOrder(start: Point, venues: List<FoodPlace>): List<FoodPlace> {
    if (venues.isEmpty()) return emptyList()
    val remaining = venues.toMutableList()
    val result = mutableListOf<FoodPlace>()
    var cur = start
    while (remaining.isNotEmpty()) {
        val nearest = remaining.minByOrNull { gridEuclideanDistance(cur, it.point) } ?: break
        result.add(nearest)
        remaining.remove(nearest)
        cur = nearest.point
    }
    return result
}