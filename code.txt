import java.util.*;
import java.io.*;

class GameException extends Exception {
    public GameException(String message) { super(message); }
    public GameException(String message, Throwable cause) { super(message, cause); }
}

class IllegalActionException extends GameException {
    public IllegalActionException(String message) { super(message); }
}


class InvalidPlacementException extends GameException {
    public InvalidPlacementException(String message) { super(message); }
}

class InsufficientResourcesException extends GameException {
    public InsufficientResourcesException(String message) { super(message); }
}

class IllegalTradeException extends GameException {
    public IllegalTradeException(String message) { super(message); }
}


class PersistenceException extends GameException {
    public PersistenceException(String message, Throwable cause) { super(message, cause); }
}

enum Resource {
    CAPITAL("Capital"),
    TALENT("Talent"),
    CLOUD("Cloud"),
    DATA("Data"),
    PATENT("Patent");

    private final String display;
    Resource(String display) { this.display = display; }
    public String display() { return display; }
}

enum SectorType {
    HUB_AI("Hub AI", Resource.TALENT),
    DISTRICT_FINTECH("District Fintech", Resource.CAPITAL),
    CAMPUS_CLOUD("Campus Cloud", Resource.CLOUD),
    QUARTER_IP("Quarter IP", Resource.PATENT),
    VALLEY_DATA("Valley Data", Resource.DATA),
    ZONE_REGULATORY("Zone Regulatory", null);

    private final String display;
    private final Resource produces;

    SectorType(String display, Resource produces) {
        this.display = display;
        this.produces = produces;
    }

    public String display() { return display; }
    public Resource produces() { return produces; }
    public boolean isProductive() { return produces != null; }

    public static SectorType[] productiveTypes()
    {
        return new SectorType[] {
            HUB_AI, DISTRICT_FINTECH, CAMPUS_CLOUD, QUARTER_IP, VALLEY_DATA
        };
    }

}

enum FounderRole {
    NONE("No Role", 0),
    CEO_HACKER("CEO Hacker", GameConstants.ROLE_SCORE_PENALTY),
    TECH_GURU("Tech Guru (CTO)", GameConstants.ROLE_SCORE_PENALTY),
    VC_FUNDED("VC-Funded", GameConstants.ROLE_SCORE_PENALTY);

    private final String display;
    private final int scorePenalty;

    FounderRole(String display, int scorePenalty)
    {
        this.display = display;
        this.scorePenalty = scorePenalty;
    }

    public String display() { return display; }
    public int scorePenalty() { return scorePenalty; }

    public int crisisCardLimit()
    {
        switch (this)
        {
            case TECH_GURU:
            case VC_FUNDED:
                return GameConstants.CRISIS_LIMIT_PRIVILEGED;
            default:
                return GameConstants.CRISIS_LIMIT_DEFAULT;
        }
    }

    public int marketTradeRatio()
    {
        return this == CEO_HACKER
                ? GameConstants.MARKET_RATIO_HACKER
                : GameConstants.MARKET_RATIO_STANDARD;
    }
}


class GameConstants {

    private GameConstants() { }

    // --- Dice ---
    public static final int DICE_MIN_FACE = 1;
    public static final int DICE_MAX_FACE = 6;
    public static final int DICE_COUNT = 2;
    public static final int CRISIS_DICE_SUM = 7;
    public static final int MIN_DICE_SUM = 2;
    public static final int MAX_DICE_SUM = 12;

    // --- Victory ---
    public static final int VICTORY_POINTS = 10;

    // --- Structure point values ---
    public static final int MVP_POINTS = 1;
    public static final int UNICORN_POINTS = 2;

    // --- Crisis ---
    public static final int CRISIS_LIMIT_DEFAULT = 7;
    public static final int CRISIS_LIMIT_PRIVILEGED = 9;
    public static final int CRISIS_DISCARD_DIVISOR = 2;

    // --- Roles ---
    public static final int ROLE_SCORE_PENALTY = -1;
    public static final int TECH_GURU_BONUS_CAPITAL = 2;
    public static final int MARKET_RATIO_STANDARD = 4;
    public static final int MARKET_RATIO_HACKER = 3;
    public static final int HACKER_UNICORN_CLOUD_DISCOUNT = 1;

    // --- Market pricing ---
    public static final int MARKET_BASE_PRICE = 4;
    public static final int MARKET_MAX_PRICE = 6;
    public static final int MARKET_MIN_PRICE = 2;
    public static final int MARKET_PRICE_STEP = 1;
    public static final int MARKET_IDLE_TURNS_FOR_DROP = 3;

    // --- Map dimensions (square grid of sectors) ---
    public static final int MAP_ROWS = 5;
    public static final int MAP_COLS = 5;

    // --- Build costs (immutable) ---
    public static final Map<Resource, Integer> MVP_COST;
    public static final Map<Resource, Integer> UNICORN_COST;
    public static final Map<Resource, Integer> PARTNERSHIP_COST;

    static {
        Map<Resource, Integer> mvp = new EnumMap<>(Resource.class);
        mvp.put(Resource.CAPITAL, 1);
        mvp.put(Resource.TALENT, 1);
        mvp.put(Resource.CLOUD, 1);
        mvp.put(Resource.DATA, 1);
        MVP_COST = Collections.unmodifiableMap(mvp);

        Map<Resource, Integer> unicorn = new EnumMap<>(Resource.class);
        unicorn.put(Resource.DATA, 3);
        unicorn.put(Resource.CLOUD, 2);
        UNICORN_COST = Collections.unmodifiableMap(unicorn);

        Map<Resource, Integer> partnership = new EnumMap<>(Resource.class);
        partnership.put(Resource.CAPITAL, 1);
        partnership.put(Resource.PATENT, 1);
        PARTNERSHIP_COST = Collections.unmodifiableMap(partnership);
    }

    public static final int DEFAULT_CARD_LIMIT = CRISIS_LIMIT_DEFAULT;
    public static final int RAISED_CARD_LIMIT = CRISIS_LIMIT_PRIVILEGED;
    public static final int LONGEST_NETWORK_POINTS = 2;
    public static final int ROLE_PENALTY = ROLE_SCORE_PENALTY;
    public static final int BASE_PRICE = MARKET_BASE_PRICE;
    public static final int MAX_PRICE = MARKET_MAX_PRICE;
    public static final int MIN_PRICE = MARKET_MIN_PRICE;
    public static final int PRICE_DECAY_TURNS = MARKET_IDLE_TURNS_FOR_DROP;
    public static final int MIN_ACTIVATION = MIN_DICE_SUM;
    public static final int MAX_ACTIVATION = MAX_DICE_SUM;
    public static final int CRISIS_ROLL = CRISIS_DICE_SUM;
    public static final int LONGEST_NETWORK_MIN = 3;

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

}

abstract class CompanyStructure implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final int ownerId;

    protected CompanyStructure(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public abstract Map<Resource, Integer> produce(Resource resource);

    public abstract int getVictoryPoints();

    public abstract String typeName();
}

class MVP extends CompanyStructure {
    private static final long serialVersionUID = 1L;
    private static final int PRODUCTION_UNITS = 1;

    public MVP(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        Map<Resource, Integer> out = new EnumMap<>(Resource.class);
        if (resource != null) {
            out.put(resource, PRODUCTION_UNITS);
        }
        return out;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.MVP_POINTS;
    }

    @Override
    public String typeName() {
        return "MVP";
    }
}

class Unicorn extends CompanyStructure {
    private static final long serialVersionUID = 1L;
    private static final int PRODUCTION_UNITS = 2;

    public Unicorn(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        Map<Resource, Integer> out = new EnumMap<>(Resource.class);
        if (resource != null) {
            out.put(resource, PRODUCTION_UNITS);
        }
        return out;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.UNICORN_POINTS;
    }

    @Override
    public String typeName() {
        return "Unicorn";
    }
}

class Partnership extends CompanyStructure {
    private static final long serialVersionUID = 1L;

    public Partnership(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        return Collections.emptyMap();
    }

    @Override
    public int getVictoryPoints() {
        return 0;
    }

    @Override
    public String typeName() {
        return "Partnership";
    }
}

class Sector implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;
    private final SectorType type;
    private final int activationNumber;
    private boolean inspected;

    public Sector(int row, int col, SectorType type, int activationNumber) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.activationNumber = activationNumber;
        this.inspected = false;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public SectorType getType() { return type; }
    public int getActivationNumber() { return activationNumber; }

    public boolean isInspected() { return inspected; }
    public void setInspected(boolean inspected) { this.inspected = inspected; }

    public boolean producesOn(int diceSum) {
        return type.isProductive() && !inspected && activationNumber == diceSum;
    }

    @Override
    public String toString() {
        return type.display() + (type.isProductive() ? " [" + activationNumber + "]" : "");
    }
}

class Vertex implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final List<Sector> adjacentSectors;
    private final List<Integer> adjacentVertexIds;
    private CompanyStructure structure;

    public Vertex(int id) {
        this.id = id;
        this.adjacentSectors = new ArrayList<>();
        this.adjacentVertexIds = new ArrayList<>();
        this.structure = null;
    }

    public int getId() { return id; }
    public List<Sector> getAdjacentSectors() { return adjacentSectors; }
    public List<Integer> getAdjacentVertexIds() { return adjacentVertexIds; }

    public CompanyStructure getStructure() { return structure; }
    public void setStructure(CompanyStructure structure) { this.structure = structure; }
    public boolean isOccupied() { return structure != null; }

    public void linkSector(Sector sector) {
        if (sector != null && !adjacentSectors.contains(sector)) {
            adjacentSectors.add(sector);
        }
    }

    public void linkVertex(int otherId) {
        if (otherId != id && !adjacentVertexIds.contains(otherId)) {
            adjacentVertexIds.add(otherId);
        }
    }
}

class Edge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int vertexA;
    private final int vertexB;
    private Partnership partnership;

    public Edge(int id, int vertexA, int vertexB) {
        this.id = id;
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.partnership = null;
    }

    public int getId() { return id; }
    public int getVertexA() { return vertexA; }
    public int getVertexB() { return vertexB; }

    public Partnership getPartnership() { return partnership; }
    public void setPartnership(Partnership partnership) { this.partnership = partnership; }
    public boolean hasPartnership() { return partnership != null; }


    public boolean connectsBoth(int a, int b)
    {
        return (vertexA == a && vertexB == b) || (vertexA == b && vertexB == a);
    }


    public boolean connects(int vertexId) {
        return vertexA == vertexId || vertexB == vertexId;
    }

    public int other(int vertexId) {
        if (vertexA == vertexId) return vertexB;
        if (vertexB == vertexId) return vertexA;
        return -1;
    }
}

class ResourceBank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Resource, Integer> cards;

    public ResourceBank() {
        cards = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            cards.put(r, 0);
        }
    }

    public int get(Resource resource) {
        return cards.getOrDefault(resource, 0);
    }

    public void add(Resource resource, int amount) {
        cards.put(resource, get(resource) + amount);
    }

    public boolean has(Resource resource, int amount) {
        return get(resource) >= amount;
    }

    public boolean remove(Resource resource, int amount) {
        if (!has(resource, amount)) {
            return false;
        }
        cards.put(resource, get(resource) - amount);
        return true;
    }

    public int total() {
        int sum = 0;
        for (int v : cards.values()) {
            sum += v;
        }
        return sum;
    }

    public Map<Resource, Integer> snapshot() {
        return new EnumMap<>(cards);
    }

    public boolean canAfford(Map<Resource, Integer> cost) {
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            if (get(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void pay(Map<Resource, Integer> cost) {
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            remove(e.getKey(), e.getValue());
        }
    }
}

class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int pendingTax;
    private final int id;
    private final String name;
    private final boolean ai;
    private final ResourceBank bank;
    private final List<Vertex> ownedVertices;
    private final List<Edge> ownedEdges;
    private FounderRole role;
    private int longestNetworkAchievedAtTurn;
    private boolean holdsLongestNetwork;

    public Player(int id, String name, boolean ai) {
        this.id = id;
        this.pendingTax = 0;
        this.name = name;
        this.ai = ai;
        this.bank = new ResourceBank();
        this.ownedVertices = new ArrayList<>();
        this.ownedEdges = new ArrayList<>();
        this.role = FounderRole.NONE;
        this.longestNetworkAchievedAtTurn = Integer.MAX_VALUE;
        this.holdsLongestNetwork = false;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isAi() { return ai; }
    public ResourceBank getBank() { return bank; }
    public List<Vertex> getOwnedVertices() { return ownedVertices; }
    public List<Edge> getOwnedEdges() { return ownedEdges; }

    public FounderRole getRole() { return role; }
    public void setRole(FounderRole role) { this.role = role; }

    public boolean holdsLongestNetwork() { return holdsLongestNetwork; }
    public void setHoldsLongestNetwork(boolean v) { this.holdsLongestNetwork = v; }

    public int getLongestNetworkAchievedAtTurn() { return longestNetworkAchievedAtTurn; }
    public void setLongestNetworkAchievedAtTurn(int turn) { this.longestNetworkAchievedAtTurn = turn; }

    public int getPendingTax() { return pendingTax; }
    public void markPendingTax(int amount) { this.pendingTax = amount; }
    public void clearPendingTax() { this.pendingTax = 0; }

    public void addResource(Resource r, int amount) { bank.add(r, amount); }
    public boolean removeResource(Resource r, int amount) { return bank.remove(r, amount); }
    public int getResource(Resource r) { return bank.get(r); }
    public int totalCards() { return bank.total(); }

    public int getCardLimit() {
        if (role == FounderRole.TECH_GURU || role == FounderRole.VC_FUNDED) {
            return GameConstants.RAISED_CARD_LIMIT;
        }
        return GameConstants.DEFAULT_CARD_LIMIT;
    }

    public void registerStructureVertex(Vertex v) {
        if (!ownedVertices.contains(v)) {
            ownedVertices.add(v);
        }
    }

    public void registerEdge(Edge e) {
        if (!ownedEdges.contains(e)) {
            ownedEdges.add(e);
        }
    }

    public void grantProduction(Map<Resource, Integer> produced) {
        for (Map.Entry<Resource, Integer> e : produced.entrySet()) {
            bank.add(e.getKey(), e.getValue());
        }
    }

    public int computeScore() {
        int score = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() != null) {
                score += v.getStructure().getVictoryPoints();
            }
        }
        if (holdsLongestNetwork) {
            score += GameConstants.LONGEST_NETWORK_POINTS;
        }
        if (role != FounderRole.NONE) {
            score += GameConstants.ROLE_PENALTY;
        }
        return score;
    }

    public int mvpCount() {
        int c = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() instanceof MVP) c++;
        }
        return c;
    }

    public int unicornCount() {
        int c = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() instanceof Unicorn) c++;
        }
        return c;
    }
}

class Market implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Resource, Integer> prices;
    private final Map<Resource, Integer> turnsSinceLastBuy;

    public Market() {
        prices = new EnumMap<>(Resource.class);
        turnsSinceLastBuy = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            prices.put(r, GameConstants.BASE_PRICE);
            turnsSinceLastBuy.put(r, 0);
        }
    }

    public int priceOf(Resource resource) {
        return prices.get(resource);
    }

    public Map<Resource, Integer> snapshot() {
        return new EnumMap<>(prices);
    }

    public void onBuy(Resource resource) {
        int p = Math.min(GameConstants.MAX_PRICE, prices.get(resource) + 1);
        prices.put(resource, p);
        turnsSinceLastBuy.put(resource, 0);
    }

    public void onTurnElapsed() {
        for (Resource r : Resource.values()) {
            int counter = turnsSinceLastBuy.get(r) + 1;
            if (counter >= GameConstants.PRICE_DECAY_TURNS) {
                int p = Math.max(GameConstants.MIN_PRICE, prices.get(r) - 1);
                prices.put(r, p);
                turnsSinceLastBuy.put(r, 0);
            } else {
                turnsSinceLastBuy.put(r, counter);
            }
        }
    }
}

class GameMap implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int rows;
    private final int cols;
    private final Sector[][] sectors;
    private final Map<Integer, Vertex> vertices;
    private final Map<Integer, Edge> edges;
    private final Map<Long, Integer> vertexKeyToId;

    public GameMap(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.sectors = new Sector[rows][cols];
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        this.vertexKeyToId = new HashMap<>();
        generate(new Random(seed));
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Sector[][] getSectors() { return sectors; }
    public Map<Integer, Vertex> getVertices() { return vertices; }
    public Map<Integer, Edge> getEdges() { return edges; }

    public List<Resource> adjacentResources(int vertexId) {
        List<Resource> out = new ArrayList<>();
        for (Sector s : vertices.get(vertexId).getAdjacentSectors()) {
            if (s.getType().isProductive()) out.add(s.getType().produces());
        }
        return out;
    }

    public List<Sector> sectorsWithActivation(int diceSum) {
        List<Sector> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (sectors[r][c].getActivationNumber() == diceSum
                        && sectors[r][c].getType().isProductive()) {
                    out.add(sectors[r][c]);
                }
            }
        }
        return out;
    }

    public List<Vertex> verticesAround(Sector sector) {
        List<Vertex> out = new ArrayList<>();
        for (Vertex v : vertices.values()) {
            if (v.getAdjacentSectors().contains(sector)) out.add(v);
        }
        return out;
    }

    public boolean sectorHasCompany(Sector sector) {
        for (Vertex v : verticesAround(sector)) {
            if (v.isOccupied()) return true;
        }
        return false;
    }

    public boolean anySectorOccupied() {
        for (Vertex v : vertices.values()) {
            if (v.isOccupied()) return true;
        }
        return false;
    }


    private void generate(Random random) {
        List<SectorType> pool = buildTypePool(random);
        List<Integer> numbers = buildNumberPool(random);
        int poolIndex = 0;
        int numberIndex = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SectorType type = pool.get(poolIndex % pool.size());
                poolIndex++;
                int number;
                if (type.isProductive()) {
                    number = numbers.get(numberIndex % numbers.size());
                    numberIndex++;
                } else {
                    number = 0;
                }
                sectors[r][c] = new Sector(r, c, type, number);
            }
        }
        buildVerticesAndEdges();
    }

    private List<SectorType> buildTypePool(Random random) {
        List<SectorType> pool = new ArrayList<>();
        SectorType[] productive = SectorType.productiveTypes();
        int totalCells = rows * cols;
        int regulatoryCount = Math.max(1, totalCells / 7);
        for (int i = 0; i < regulatoryCount; i++) {
            pool.add(SectorType.ZONE_REGULATORY);
        }
        int remaining = totalCells - regulatoryCount;
        int idx = 0;
        for (int i = 0; i < remaining; i++) {
            pool.add(productive[idx % productive.length]);
            idx++;
        }
        Collections.shuffle(pool, random);
        return pool;
    }

    private List<Integer> buildNumberPool(Random random) {
        List<Integer> numbers = new ArrayList<>();
        int total = rows * cols;
        for (int i = 0; i < total; i++) {
            int n = GameConstants.MIN_ACTIVATION + random.nextInt(GameConstants.MAX_ACTIVATION - GameConstants.MIN_ACTIVATION + 1);
            if (n == GameConstants.CRISIS_ROLL) {
                n = n == GameConstants.MAX_ACTIVATION ? n - 1 : n + 1;
            }
            numbers.add(n);
        }
        Collections.shuffle(numbers, random);
        return numbers;
    }

    private long vertexKey(int r, int c) {
        return ((long) r << 32) | (c & 0xffffffffL);
    }

        private int obtainVertex(int r, int c) {
        long key = vertexKey(r, c);
        Integer existing = vertexKeyToId.get(key);
        if (existing != null) {
            return existing;
        }
        int id = vertices.size();
        Vertex v = new Vertex(id);
        vertices.put(id, v);
        v.linkVertex(obtainVertexPreemptive(r - 1, c));
        v.linkVertex(obtainVertexPreemptive(r + 1, c));
        v.linkVertex(obtainVertexPreemptive(r, c - 1));
        v.linkVertex(obtainVertexPreemptive(r, c + 1));
        vertexKeyToId.put(key, id);
        return id;
    }

    private int obtainVertexPreemptive(int r, int c) {
        if (r < 0 || r > rows || c < 0 || c > cols) return -1;
        long key = vertexKey(r, c);
        Integer id = vertexKeyToId.get(key);
        return (id != null) ? id : -1;
    }

    private void buildVerticesAndEdges() {
        for (int r = 0; r <= rows; r++) {
            for (int c = 0; c <= cols; c++) {
                int vid = obtainVertex(r, c);
                Vertex v = vertices.get(vid);
                if (r > 0 && c > 0) v.linkSector(sectors[r - 1][c - 1]);
                if (r > 0 && c < cols) v.linkSector(sectors[r - 1][c]);
                if (r < rows && c > 0) v.linkSector(sectors[r][c - 1]);
                if (r < rows && c < cols) v.linkSector(sectors[r][c]);
            }
        }
        Set<String> seenEdges = new HashSet<>();
        for (Vertex v : vertices.values()) {
            for (int neighborId : v.getAdjacentVertexIds()) {
                if (neighborId == -1) continue;
                int low = Math.min(v.getId(), neighborId);
                int high = Math.max(v.getId(), neighborId);
                String edgeKey = low + "-" + high;
                if (!seenEdges.contains(edgeKey)) {
                    int eid = edges.size();
                    edges.put(eid, new Edge(eid, low, high));
                    seenEdges.add(edgeKey);
                }
            }
        }
    }

    public List<Edge> getVertexEdges(int vertexId) {
        List<Edge> out = new ArrayList<>();
        for (Edge e : edges.values()) {
            if (e.connects(vertexId)) out.add(e);
        }
        return out;
    }

    public List<Integer> neighborsOf(int vertexId) {
        Vertex v = vertices.get(vertexId);
        List<Integer> out = new ArrayList<>();
        for (int id : v.getAdjacentVertexIds()) {
            if (id != -1) out.add(id);
        }
        return out;
    }

    public int distanceInVertices(int fromVertexId, int toVertexId) {
        if (fromVertexId == toVertexId) return 0;
        Deque<int[]> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(new int[]{fromVertexId, 0});
        visited.add(fromVertexId);
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int next : neighborsOf(cur[0])) {
                if (visited.contains(next)) continue;
                if (next == toVertexId) return cur[1] + 1;
                visited.add(next);
                queue.add(new int[]{next, cur[1] + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    public Edge edgeBetween(int a, int b) {
        for (Edge e : edges.values()) {
            if (e.connectsBoth(a, b)) return e;
        }
        return null;
    }
}

class GameController implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final GameMap gameMap;
    private final Market market;
    private final List<Player> players;
    private final Random random;

    private int currentPlayerIndex;
    private int turnCount;
    private int lastDiceRoll;
    private Sector inspectorSector;
    private boolean gameOver;
    private Player winner;
    private int longestNetworkHolder;
    private int longestNetworkLength;

    private final List<String> eventLog;

    public GameController(List<String> playerNames, long seed)
    {
        if (playerNames == null || playerNames.size() < GameConstants.MIN_PLAYERS
                || playerNames.size() > GameConstants.MAX_PLAYERS)
        {
            throw new IllegalArgumentException("Player count must be between "
                    + GameConstants.MIN_PLAYERS + " and " + GameConstants.MAX_PLAYERS);
        }
        this.random = new Random(seed);
        this.gameMap = new GameMap(GameConstants.MAP_ROWS, GameConstants.MAP_COLS, seed);
        this.market = new Market();
        this.players = new ArrayList<>();
        for (int i = 0; i < playerNames.size(); i++)
            players.add(new Player(i, playerNames.get(i), false));
        this.currentPlayerIndex = 0;
        this.turnCount = 0;
        this.lastDiceRoll = 0;
        this.inspectorSector = null;
        this.gameOver = false;
        this.winner = null;
        this.longestNetworkHolder = -1;
        this.longestNetworkLength = 0;
        this.eventLog = new ArrayList<>();
    }

    private void validateVertexForBuild(Player player, int vertexId, boolean setup) throws GameException {
        Vertex v = gameMap.getVertices().get(vertexId);
        if (v == null) throw new InvalidPlacementException("Unknown vertex");
        if (v.isOccupied()) throw new InvalidPlacementException("Vertex already occupied");
        for (Vertex other : gameMap.getVertices().values()) {
            if (other.isOccupied()
                    && gameMap.distanceInVertices(vertexId, other.getId()) < 2) {
                throw new InvalidPlacementException("Must be at least 2 edges from another company");
            }
        }
    }

    private void validateEdgeForSetup(Player player, int edgeId, int anchorVertexId) throws GameException {
        Edge e = gameMap.getEdges().get(edgeId);
        if (e == null) throw new InvalidPlacementException("Unknown edge");
        if (e.hasPartnership()) throw new InvalidPlacementException("Edge already has a Partnership");
        if (!e.connects(anchorVertexId)) {
            throw new InvalidPlacementException("Setup Partnership must touch the placed MVP");
        }
    }


    public synchronized void assignRole(int playerId, FounderRole role) throws Exception
    {
        if (role != FounderRole.NONE)
        {
            for (Player p : players)
            {
                if (p.getRole() == role)
                {
                    throw new IllegalActionException("Role " + role.display() + " is already taken");
                }
            }
        }
        Player player = players.get(playerId);
        player.setRole(role);
        if (role == FounderRole.TECH_GURU)
        {
            player.addResource(Resource.CAPITAL, GameConstants.TECH_GURU_BONUS_CAPITAL);
        }
        log(player.getName() + " selected role " + role.display());
    }

    private void updateLongestNetwork()
    {
        int bestLen = 0;
        Player bestPlayer = null;
        for (Player p : players)
        {
            int len = longestPartnershipChain(p);
            if (len >= GameConstants.LONGEST_NETWORK_MIN)
            {
                if (len > bestLen)
                {
                    bestLen = len;
                    bestPlayer = p;
                }
                else if (len == bestLen && bestPlayer != null
                        && p.getLongestNetworkAchievedAtTurn() < bestPlayer.getLongestNetworkAchievedAtTurn())
                {
                    bestPlayer = p;
                }
            }
        }
        for (Player p : players)
        {
            boolean now = p == bestPlayer;
            if (now && !p.holdsLongestNetwork())
            {
                p.setLongestNetworkAchievedAtTurn(turnCount);
            }
            p.setHoldsLongestNetwork(now);
        }
        if (bestPlayer != null)
        {
            longestNetworkHolder = bestPlayer.getId();
            longestNetworkLength = bestLen;
        }
        else
        {
            longestNetworkHolder = -1;
            longestNetworkLength = 0;
        }
    }

    private int longestPartnershipChain(Player player)
    {
        Set<Integer> ownedEdgeIds = new HashSet<>();
        Set<Integer> endpointVertices = new HashSet<>();
        for (Edge e : player.getOwnedEdges())
        {
            ownedEdgeIds.add(e.getId());
            endpointVertices.add(e.getVertexA());
            endpointVertices.add(e.getVertexB());
        }
        int best = 0;
        for (int start : endpointVertices)
        {
            best = Math.max(best, walkChain(start, ownedEdgeIds, new HashSet<Integer>()));
        }
        return best;
    }

    private int walkChain(int vertexId, Set<Integer> ownedEdgeIds, Set<Integer> usedEdges)
    {
        int best = 0;
        for (Edge e : gameMap.getVertexEdges(vertexId))
        {
            if (!ownedEdgeIds.contains(e.getId())) continue;
            if (usedEdges.contains(e.getId())) continue;
            usedEdges.add(e.getId());
            int sub = 1 + walkChain(e.other(vertexId), ownedEdgeIds, usedEdges);
            usedEdges.remove(e.getId());
            if (sub > best) best = sub;
        }
        return best;
    }


    public synchronized void setupInitialPlacement(int playerId, int vertexId, int edgeId, boolean grantResources) throws GameException
    {
        Player player = players.get(playerId);
        validateVertexForBuild(player, vertexId, true);
        MVP mvp = new MVP(playerId);
        gameMap.getVertices().get(vertexId).setStructure(mvp);
        Vertex vtx = gameMap.getVertices().get(vertexId);
        vtx.setStructure(mvp);
        player.registerStructureVertex(vtx);

        validateEdgeForSetup(player, edgeId, vertexId);
        Partnership partnership = new Partnership(playerId);
        gameMap.getEdges().get(edgeId).setPartnership(partnership);
        Edge edg = gameMap.getEdges().get(edgeId);
        edg.setPartnership(partnership);
        player.registerEdge(edg);

        if (grantResources)
        {
            for (Resource r : gameMap.adjacentResources(vertexId))
            {
                player.addResource(r, 1);
            }
            log(player.getName() + " received starting resources from second MVP");
        }
        updateLongestNetwork();
        log(player.getName() + " placed setup MVP at vertex " + vertexId
                + " and Partnership at edge " + edgeId);
    }

    // ---------------- Build: MVP ----------------
    public synchronized void buildMVP(int playerId, int vertexId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        // placement validity: known, unoccupied, distance-2 rule
        validateVertexForBuild(player, vertexId, false);

        // must be afforded
        if (!player.getBank().canAfford(GameConstants.MVP_COST)) {
            throw new InsufficientResourcesException("Not enough resources to build an MVP");
        }

        player.getBank().pay(GameConstants.MVP_COST);
        Vertex vtx = gameMap.getVertices().get(vertexId);
        vtx.setStructure(new MVP(playerId));
        player.registerStructureVertex(vtx);

        log(player.getName() + " built an MVP at vertex " + vertexId);
        checkVictory(player);
    }

    // ---------------- Upgrade: MVP -> Unicorn ----------------
    public synchronized void upgradeToUnicorn(int playerId, int vertexId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        Vertex vtx = gameMap.getVertices().get(vertexId);
        if (vtx == null) throw new InvalidPlacementException("Unknown vertex");

        CompanyStructure cs = vtx.getStructure();
        if (!(cs instanceof MVP)) {
            throw new IllegalActionException("Only an existing MVP can be upgraded to a Unicorn");
        }
        if (cs.getOwnerId() != playerId) {
            throw new IllegalActionException("Cannot upgrade an MVP you do not own");
        }

        // CEO Hacker pays 1 less Cloud
        Map<Resource, Integer> cost = new EnumMap<>(GameConstants.UNICORN_COST);
        if (player.getRole() == FounderRole.CEO_HACKER) {
            int cloud = cost.getOrDefault(Resource.CLOUD, 0)
                    - GameConstants.HACKER_UNICORN_CLOUD_DISCOUNT;
            cost.put(Resource.CLOUD, Math.max(0, cloud));
        }

        if (!player.getBank().canAfford(cost)) {
            throw new InsufficientResourcesException("Not enough resources to upgrade to a Unicorn");
        }

        player.getBank().pay(cost);
        vtx.setStructure(new Unicorn(playerId));
        // vertex already registered when MVP was built; keep registration idempotent
        player.registerStructureVertex(vtx);

        log(player.getName() + " upgraded an MVP to a Unicorn at vertex " + vertexId);
        checkVictory(player);
    }

    // ---------------- Build: Partnership ----------------
    public synchronized void buildPartnership(int playerId, int edgeId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        Edge edge = gameMap.getEdges().get(edgeId);
        if (edge == null) throw new InvalidPlacementException("Unknown edge");
        if (edge.hasPartnership()) {
            throw new InvalidPlacementException("Edge already has a Partnership");
        }

        // connection rule: must touch a vertex the player already controls
        // (an owned structure vertex, or an endpoint of an owned Partnership edge)
        if (!isConnectedToPlayer(player, edge)) {
            throw new InvalidPlacementException(
                    "Partnership must connect to your existing network");
        }

        if (!player.getBank().canAfford(GameConstants.PARTNERSHIP_COST)) {
            throw new InsufficientResourcesException("Not enough resources to build a Partnership");
        }

        player.getBank().pay(GameConstants.PARTNERSHIP_COST);
        edge.setPartnership(new Partnership(playerId));
        player.registerEdge(edge);

        updateLongestNetwork();
        log(player.getName() + " built a Partnership at edge " + edgeId);
        checkVictory(player);
    }

    private boolean isConnectedToPlayer(Player player, Edge edge) {
        int a = edge.getVertexA();
        int b = edge.getVertexB();
        // touches an owned structure
        for (Vertex v : player.getOwnedVertices()) {
            if (v.getId() == a || v.getId() == b) return true;
        }
        // touches an endpoint of an owned Partnership edge
        for (Edge owned : player.getOwnedEdges()) {
            if (owned.connects(a) || owned.connects(b)) return true;
        }
        return false;
    }

    // ---------------- Trade with the Market ----------------
    public synchronized void tradeWithMarket(int playerId, Resource give, Resource receive)
            throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        if (give == receive) {
            throw new IllegalTradeException("Cannot trade a resource for itself");
        }
        Player player = players.get(playerId);

        int ratio = player.getRole().marketTradeRatio(); // 4:1 default, 3:1 for CEO Hacker
        if (!player.getBank().has(give, ratio)) {
            throw new InsufficientResourcesException(
                    "Need " + ratio + " " + give.display() + " to trade for 1 " + receive.display());
        }

        player.removeResource(give, ratio);
        player.addResource(receive, 1);
        market.onBuy(receive);

        log(player.getName() + " traded " + ratio + " " + give.display()
                + " for 1 " + receive.display());
    }


    public synchronized int rollDice() throws GameException
    {
        if (gameOver)
        {
            throw new IllegalActionException("Game is already over");
        }
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        lastDiceRoll = d1 + d2;
        log(currentPlayer().getName() + " rolled " + d1 + " + " + d2 + " = " + lastDiceRoll);
        if (lastDiceRoll == GameConstants.CRISIS_DICE_SUM)
        {
            triggerCrisisTax();
        }
        else
        {
            produceResources(lastDiceRoll);
        }
        market.onTurnElapsed();
        return lastDiceRoll;
    }

    private void produceResources(int diceSum) {
        for (Sector sector : gameMap.sectorsWithActivation(diceSum)) {
            if (sector == inspectorSector) {
                log("Sector blocked by inspector, no production");
                continue;
            }
            Resource produced = sector.getType().produces();
            if (produced == null) continue;
            for (Vertex v : gameMap.verticesAround(sector)) {
                CompanyStructure cs = v.getStructure();
                if (cs != null) {
                    Map<Resource, Integer> out = cs.produce(produced);
                    Player owner = players.get(cs.getOwnerId());
                    owner.grantProduction(out);
                    Integer amt = out.get(produced);
                    if (amt != null) {
                        log(owner.getName() + " gained " + amt + " " + produced.display());
                    }
                }
            }
        }
    }


    private void triggerCrisisTax()
    {
        log("Regulatory crisis! Dice sum is 7");
        for (Player p : players)
        {
            int limit = p.getRole().crisisCardLimit();
            int total = p.totalCards();
            if (total > limit)
            {
                int toReturn = total/GameConstants.CRISIS_DISCARD_DIVISOR;
                p.markPendingTax(toReturn);
                log(p.getName() + " must return " + toReturn + " cards (has " + total + ")");
            }
        }
    }

    public synchronized void applyCrisisTax(int playerId, java.util.Map<Resource, Integer> returned) throws Exception
    {
        Player player = players.get(playerId);
        int required = player.getPendingTax();
        int sum = 0;
        for (int v : returned.values())
        {
            sum += v;
        }
        if (sum != required)
        {
            throw new IllegalActionException("Must return exactly " + required + " cards");
        }
        for (java.util.Map.Entry<Resource, Integer> e : returned.entrySet())
        {
            if (player.getResource(e.getKey()) < e.getValue())
            {
                throw new InsufficientResourcesException("Cannot return more cards than held");
            }
        }
        for (java.util.Map.Entry<Resource, Integer> e : returned.entrySet())
        {
            player.removeResource(e.getKey(), e.getValue());
        }
        player.clearPendingTax();
        log(player.getName() + " returned " + required + " cards to the bank");
    }

    public synchronized void placeInspector(int playerId, int row, int col) throws Exception {
        if (lastDiceRoll != GameConstants.CRISIS_DICE_SUM) {
            throw new IllegalActionException("Inspector can only move during a crisis");
        }
        Sector target = gameMap.getSectors()[row][col];
        boolean anyOccupied = gameMap.anySectorOccupied();
        if (anyOccupied && !gameMap.sectorHasCompany(target)) {
            throw new InvalidPlacementException("Inspector must go on an occupied sector");
        }
        inspectorSector = target;
        target.setInspected(true);
        log(players.get(playerId).getName() + " placed the inspector");
    }


    private void checkVictory(Player player) {
        if (player.computeScore() >= GameConstants.VICTORY_POINTS) {
            gameOver = true;
            winner = player;
            log(player.getName() + " has won with " + player.computeScore() + " points!");
        }
    }

    // ---------------- Turn Management ----------------

    /**
     * Ends the current player's turn. 
     * According to page 11, victory is checked at the end of the turn.
     */
    public synchronized void endTurn() {
        if (gameOver) return;

        Player player = currentPlayer();
        
        // Final victory check before passing the turn
        if (player.computeScore() >= GameConstants.VICTORY_POINTS) {
            gameOver = true;
            winner = player;
            log(player.getName() + " wins the game with " + player.computeScore() + " points!");
            return;
        }

        // Advance to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        
        // If we returned to the first player, a full round (turn) has passed
        if (currentPlayerIndex == 0) {
            turnCount++;
        }
        
        log("It is now " + currentPlayer().getName() + "'s turn.");
    }

    // ---------------- Persistence (Save/Load) ----------------

    /**
     * Saves the entire game state to a file using Java Serialization.
     * Required for the "Persistence" requirement (Page 12).
     */
    public void saveGame(String filePath) throws GameException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            log("Game saved successfully to " + filePath);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save game: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a GameController instance from a file.
     */
    public static GameController loadGame(String filePath) throws GameException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (GameController) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Failed to load game: " + e.getMessage(), e);
        }
    }

    // ---------------- Getters for UI/Integration ----------------

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public GameMap getGameMap() { return gameMap; }
    public Market getMarket() { return market; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public int getTurnCount() { return turnCount; }
    public boolean isGameOver() { return gameOver; }
    public Player getWinner() { return winner; }
    public List<String> getEventLog() { return Collections.unmodifiableList(eventLog); }
    public Sector getInspectorSector() { return inspectorSector; }
    public int getLastDiceRoll() { return lastDiceRoll; }


    private Player currentPlayer()
    {
        return players.get(currentPlayerIndex);
    }

    private void log(String message)
    {
        eventLog.add("[Turn " + turnCount + "] " + message);
    }
}
