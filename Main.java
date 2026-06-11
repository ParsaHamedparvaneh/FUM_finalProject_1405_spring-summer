import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
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
    HUB_AI("AI", Resource.TALENT),
    DISTRICT_FINTECH("Fintech", Resource.CAPITAL),
    CAMPUS_CLOUD("Cloud", Resource.CLOUD),
    QUARTER_IP("Patent", Resource.PATENT),
    VALLEY_DATA("Data", Resource.DATA),
    ZONE_REGULATORY("Zzz", null);

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


public class Main extends Application
{
    private GameController game;
    private Thread gameThread;
    private Stage primaryStage;
    
    // UI Components
    private GridPane mapGrid;
    private VBox rightPanel;
    private TextArea eventLogArea;
    private Label currentPlayerLabel;
    private Label marketPricesLabel;
    private VBox playersResourcesBox;
    
    // Control buttons
    private Button rollButton;
    private Button endTurnButton;
    private Button buildMVPButton;
    private Button upgradeButton;
    private Button buildPartnershipButton;
    private Button tradeButton;
    private Button marketButton;
    
    private boolean waitingForCrisisPayment = false;
    private int currentCrisisPlayerIndex = 0;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }
    
    private void showMainMenu() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 40px;");
        
        Text title = new Text("Silicon Valley: The Tech Cartel");
        title.setStyle("-fx-font-size: 28px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        Button newGameBtn = new Button("New Game");
        newGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 200px;");
        newGameBtn.setOnAction(e -> showPlayerSetup());
        
        Button loadGameBtn = new Button("Load Game");
        loadGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 200px;");
        loadGameBtn.setOnAction(e -> loadGame());
        
        Button exitBtn = new Button("Exit");
        exitBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 200px;");
        exitBtn.setOnAction(e -> Platform.exit());
        
        menuBox.getChildren().addAll(title, newGameBtn, loadGameBtn, exitBtn);
        
        Scene scene = new Scene(menuBox, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley: The Tech Cartel");
        primaryStage.show();
    }
    
    private void showPlayerSetup() {
        VBox setupBox = new VBox(10);
        setupBox.setAlignment(Pos.CENTER);
        setupBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");
        
        Text title = new Text("Game Setup");
        title.setStyle("-fx-font-size: 24px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of Players:");
        countLabel.setStyle("-fx-text-fill: #eeeeee;");
        ComboBox<Integer> playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(2, 3, 4);
        playerCountCombo.setValue(2);
        playerCountBox.getChildren().addAll(countLabel, playerCountCombo);
        
        VBox nameBox = new VBox(5);
        
        Runnable updateNameFields = () -> {
            nameBox.getChildren().clear();
            int count = playerCountCombo.getValue();
            for (int i = 0; i < count; i++) {
                Label playerLabel = new Label("Player " + (i + 1) + ":");
                playerLabel.setStyle("-fx-text-fill: #eeeeee;");
                TextField tf = new TextField("Player " + (i + 1));
                nameBox.getChildren().addAll(playerLabel, tf);
            }
        };
        
        playerCountCombo.setOnAction(e -> updateNameFields.run());
        updateNameFields.run();
        
        // Role selection checkbox
        CheckBox roleToggle = new CheckBox("Enable Founder Roles (costs 1 point)");
        roleToggle.setStyle("-fx-text-fill: #eeeeee;");
        
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> {
            List<String> names = new ArrayList<>();
            int playerCount = playerCountCombo.getValue();
            for (int i = 0; i < playerCount; i++) {
                TextField tf = (TextField) nameBox.getChildren().get(i * 2 + 1);
                names.add(tf.getText());
            }
            startNewGame(names, roleToggle.isSelected());
        });
        
        setupBox.getChildren().addAll(title, playerCountBox, nameBox, roleToggle, startButton);
        
        Scene scene = new Scene(setupBox, 800, 600);
        primaryStage.setScene(scene);
    }
    
    private void startNewGame(List<String> playerNames, boolean withRoles) {
        // Run game logic in separate thread
        gameThread = new Thread(() -> {
            try {
                long seed = System.currentTimeMillis();
                game = new GameController(playerNames, seed);
                
                Platform.runLater(() -> {
                    // Show game UI first so players can see the map
                    showGameUI();
                    
                    // Then show role selection if enabled
                    if (withRoles) {
                        showRoleSelection();
                    } else {
                        // Start initial placement after UI is visible
                        Platform.runLater(() -> {
                            try {
                                performInitialPlacement();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showError("Initial placement failed: " + ex.getMessage());
                            }
                        });
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError("Failed to create game: " + ex.getMessage()));
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }
    
    private void performInitialPlacement() throws Exception {
        // Round 1: forward order - each player chooses their placement
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            
            Platform.runLater(() -> {
                showInitialPlacementDialog(player, false);
            });
            
            // Wait for player to make their choice
            while (waitingForPlacement) {
                Thread.sleep(100);
            }
            waitingForPlacement = false;
        }
        
        // Round 2: reverse order with resource grant
        for (int i = game.getPlayers().size() - 1; i >= 0; i--) {
            Player player = game.getPlayers().get(i);
            
            Platform.runLater(() -> {
                showInitialPlacementDialog(player, true);
            });
            
            while (waitingForPlacement) {
                Thread.sleep(100);
            }
            waitingForPlacement = false;
        }
    }

    private boolean waitingForPlacement = false;
    private Vertex selectedPlacementVertex = null;
    private Edge selectedPlacementEdge = null;

    private void showInitialPlacementDialog(Player player, boolean grantResources) {
        Stage placementStage = new Stage();
        placementStage.setTitle("Initial Placement - " + player.getName());
        
        VBox mainBox = new VBox(15);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");
        
        Label titleLabel = new Label(player.getName() + " - Place Your Structures");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");
        
        String roundText = grantResources ? "Round 2 (Reverse Order) - You will receive resources" : "Round 1 - Free placement";
        Label roundLabel = new Label(roundText);
        roundLabel.setStyle("-fx-text-fill: #eeeeee; -fx-font-size: 12px;");
        
        Label instructionLabel = new Label("First, select a VERTEX to place your MVP:");
        instructionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Create a scrollable list of available vertices
        ListView<String> vertexList = new ListView<>();
        List<Vertex> availableVertices = new ArrayList<>();
        
        for (Vertex v : game.getGameMap().getVertices().values()) {
            if (!v.isOccupied() && isValidPlacementForInitial(v)) {
                availableVertices.add(v);
                String coords = getVertexCoordinates(v);
                vertexList.getItems().add("Vertex " + v.getId() + " - " + coords);
            }
        }
        
        vertexList.setPrefHeight(150);
        vertexList.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: white;");
        
        Label selectedVertexLabel = new Label("No vertex selected");
        selectedVertexLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
        
        Label edgeInstructionLabel = new Label("Then, select an EDGE connected to your MVP:");
        edgeInstructionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        edgeInstructionLabel.setVisible(false);
        
        ListView<String> edgeList = new ListView<>();
        List<Edge> availableEdges = new ArrayList<>();
        edgeList.setPrefHeight(150);
        edgeList.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: white;");
        edgeList.setVisible(false);
        
        Label selectedEdgeLabel = new Label("No edge selected");
        selectedEdgeLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
        selectedEdgeLabel.setVisible(false);
        
        // Vertex selection handler
        vertexList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                int vertexId = Integer.parseInt(selected.split(" ")[1]);
                for (Vertex v : availableVertices) {
                    if (v.getId() == vertexId) {
                        selectedPlacementVertex = v;
                        selectedVertexLabel.setText("Selected: " + selected);
                        selectedVertexLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                        
                        // Now show edges connected to this vertex
                        availableEdges.clear();
                        edgeList.getItems().clear();
                        for (Edge e : game.getGameMap().getVertexEdges(vertexId)) {
                            if (!e.hasPartnership()) {
                                availableEdges.add(e);
                                String edgeInfo = "Edge " + e.getId() + " - between vertices " + e.getVertexA() + " and " + e.getVertexB();
                                edgeList.getItems().add(edgeInfo);
                            }
                        }
                        
                        if (availableEdges.isEmpty()) {
                            edgeList.getItems().add("No available edges connected to this vertex!");
                        } else {
                            edgeInstructionLabel.setVisible(true);
                            edgeList.setVisible(true);
                            selectedEdgeLabel.setVisible(true);
                        }
                        break;
                    }
                }
            }
        });
        
        // Edge selection handler
        edgeList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && !selected.equals("No available edges connected to this vertex!")) {
                int edgeId = Integer.parseInt(selected.split(" ")[1]);
                for (Edge e : availableEdges) {
                    if (e.getId() == edgeId) {
                        selectedPlacementEdge = e;
                        selectedEdgeLabel.setText("Selected: " + selected);
                        selectedEdgeLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                        break;
                    }
                }
            }
        });
        
        Button confirmBtn = new Button("Place Structures");
        confirmBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        confirmBtn.setOnAction(e -> {
            if (selectedPlacementVertex == null) {
                showError("Please select a vertex first!");
                return;
            }
            if (selectedPlacementEdge == null) {
                showError("Please select an edge connected to your vertex!");
                return;
            }
            
            try {
                game.setupInitialPlacement(player.getId(), selectedPlacementVertex.getId(), selectedPlacementEdge.getId(), grantResources);
                placementStage.close();
                waitingForPlacement = false;
                selectedPlacementVertex = null;
                selectedPlacementEdge = null;
            } catch (Exception ex) {
                showError("Placement failed: " + ex.getMessage());
            }
        });
        
        // Show a mini-map for visual reference
        GridPane miniMap = createMiniMap();
        miniMap.setAlignment(Pos.CENTER);
        miniMap.setStyle("-fx-padding: 10px; -fx-background-color: #0f3460; -fx-border-radius: 5px;");
        
        mainBox.getChildren().addAll(
            titleLabel, roundLabel, new Separator(),
            instructionLabel, vertexList, selectedVertexLabel,
            edgeInstructionLabel, edgeList, selectedEdgeLabel,
            new Separator(), miniMap, confirmBtn
        );
        
        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1a2e;");
        
        Scene scene = new Scene(scrollPane, 500, 650);
        placementStage.setScene(scene);
        placementStage.showAndWait();
    }

    private boolean isValidPlacementForInitial(Vertex vertex) {
        for (Vertex other : game.getGameMap().getVertices().values()) {
            if (other.isOccupied() && game.getGameMap().distanceInVertices(vertex.getId(), other.getId()) < 2) {
                return false;
            }
        }
        return true;
    }

    private GridPane createMiniMap() {
        GridPane miniMap = new GridPane();
        miniMap.setHgap(2);
        miniMap.setVgap(2);
        
        Sector[][] sectors = game.getGameMap().getSectors();
        for (int row = 0; row < sectors.length; row++) {
            for (int col = 0; col < sectors[0].length; col++) {
                Sector sector = sectors[row][col];
                StackPane cell = new StackPane();
                cell.setPrefSize(40, 40);
                cell.setStyle("-fx-border-color: #333; -fx-border-width: 1px;");
                
                Color bgColor = getSectorColor(sector.getType());
                Rectangle bg = new Rectangle(40, 40);
                bg.setFill(bgColor);
                
                Text symbol = new Text(sector.getType().isProductive() ? String.valueOf(sector.getActivationNumber()) : "⚖");
                symbol.setStyle("-fx-fill: WHITE; -fx-font-size: 12px;");
                
                cell.getChildren().addAll(bg, symbol);
                miniMap.add(cell, col, row);
            }
        }
        return miniMap;
    }
    
    private Vertex findValidVertexForPlacement() {
        for (Vertex v : game.getGameMap().getVertices().values()) {
            if (!v.isOccupied()) {
                boolean valid = true;
                for (Vertex other : game.getGameMap().getVertices().values()) {
                    if (other.isOccupied() && game.getGameMap().distanceInVertices(v.getId(), other.getId()) < 2) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return v;
            }
        }
        return null;
    }
    
    private Edge findAdjacentEdge(int vertexId) {
        for (Edge e : game.getGameMap().getEdges().values()) {
            if (e.connects(vertexId) && !e.hasPartnership()) {
                return e;
            }
        }
        return null;
    }
    
    private void showRoleSelection() {
        Stage roleStage = new Stage();
        VBox roleBox = new VBox(10);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");
        
        Text title = new Text("Select Founder Roles (Optional)");
        title.setStyle("-fx-font-size: 20px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        Label infoLabel = new Label("Each role can only be taken by ONE player. Roles cost -1 point.");
        infoLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
        
        List<Player> players = game.getPlayers();
        Map<Integer, String> selectedRoleNames = new HashMap<>();
        
        // Create role selection for each player using RadioButtons (no recursion issues)
        VBox playersBox = new VBox(10);
        
        // Define available roles
        String[] roleOptions = {"No Role", "CEO Hacker", "Tech Guru (CTO)", "VC-Funded"};
        String[] roleDescriptions = {
            "No special abilities",
            "Trades at 3:1 rate instead of 4:1\nUnicorn upgrade costs 1 less Cloud",
            "Starts with +2 Capital\nHolds 9 cards before tax (instead of 7)",
            "+2 starting Capital\nCard limit increased to 9 during crisis"
        };
        
        // Track which roles are taken
        Set<String> takenRoles = new HashSet<>();
        
        // Create a panel for each player
        for (int idx = 0; idx < players.size(); idx++) {
            Player player = players.get(idx);
            VBox playerBox = new VBox(5);
            playerBox.setStyle("-fx-padding: 10px; -fx-border-color: #444; -fx-border-radius: 5px; -fx-background-color: #0f3460;");
            
            Label nameLabel = new Label(player.getName());
            nameLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox optionsBox = new VBox(5);
            ToggleGroup roleGroup = new ToggleGroup();
            
            // Store radio buttons for this player
            List<RadioButton> radioButtons = new ArrayList<>();
            
            for (int i = 0; i < roleOptions.length; i++) {
                String roleName = roleOptions[i];
                RadioButton rb = new RadioButton(roleName);
                rb.setStyle("-fx-text-fill: #eeeeee;");
                rb.setToggleGroup(roleGroup);
                
                final int playerId = player.getId();
                final String selectedRole = roleName;
                
                rb.setOnAction(e -> {
                    if (rb.isSelected()) {
                        // Check if role is already taken (except "No Role")
                        if (!selectedRole.equals("No Role") && takenRoles.contains(selectedRole)) {
                            showError("Role '" + selectedRole + "' is already taken by another player!");
                            // Select "No Role" instead
                            for (RadioButton r : radioButtons) {
                                if (r.getText().equals("No Role")) {
                                    r.setSelected(true);
                                    break;
                                }
                            }
                            selectedRoleNames.put(playerId, "No Role");
                        } else {
                            // Remove old role from taken set
                            String oldRole = selectedRoleNames.get(playerId);
                            if (oldRole != null && !oldRole.equals("No Role")) {
                                takenRoles.remove(oldRole);
                            }
                            // Add new role to taken set
                            if (!selectedRole.equals("No Role")) {
                                takenRoles.add(selectedRole);
                            }
                            selectedRoleNames.put(playerId, selectedRole);
                        }
                    }
                });
                
                radioButtons.add(rb);
                optionsBox.getChildren().add(rb);
            }
            
            // Set default selection
            radioButtons.get(0).setSelected(true);
            selectedRoleNames.put(player.getId(), "No Role");
            
            // Add description area
            TextArea descArea = new TextArea();
            descArea.setEditable(false);
            descArea.setPrefHeight(60);
            descArea.setStyle("-fx-control-inner-background: #1a1a2e; -fx-text-fill: #cccccc;");
            descArea.setText(roleDescriptions[0]);
            
            // Add listener to update description
            roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    RadioButton selected = (RadioButton) newVal;
                    for (int i = 0; i < roleOptions.length; i++) {
                        if (selected.getText().equals(roleOptions[i])) {
                            descArea.setText(roleDescriptions[i]);
                            break;
                        }
                    }
                }
            });
            
            playerBox.getChildren().addAll(nameLabel, optionsBox, descArea);
            playersBox.getChildren().add(playerBox);
        }
        
        Button confirmBtn = new Button("Start Game");
        confirmBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 10px;");
        confirmBtn.setOnAction(e -> {
            // Apply selected roles
            for (Map.Entry<Integer, String> entry : selectedRoleNames.entrySet()) {
                String roleName = entry.getValue();
                if (roleName != null && !roleName.equals("No Role")) {
                    try {
                        // Convert display name to FounderRole enum
                        FounderRole role;
                        switch (roleName) {
                            case "CEO Hacker":
                                role = FounderRole.CEO_HACKER;
                                break;
                            case "Tech Guru (CTO)":
                                role = FounderRole.TECH_GURU;
                                break;
                            case "VC-Funded":
                                role = FounderRole.VC_FUNDED;
                                break;
                            default:
                                role = FounderRole.NONE;
                        }
                        if (role != FounderRole.NONE) {
                            game.assignRole(entry.getKey(), role);
                        }
                    } catch (Exception ex) {
                        showError("Failed to assign role: " + ex.getMessage());
                    }
                }
            }
            roleStage.close();
            showGameUI();
        });
        
        roleBox.getChildren().addAll(title, infoLabel, playersBox, confirmBtn);
        
        ScrollPane scrollPane = new ScrollPane(roleBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1a2e; -fx-background-color: #1a1a2e;");
        
        Scene scene = new Scene(scrollPane, 550, 550);
        roleStage.setScene(scene);
        roleStage.setTitle("Select Founder Roles");
        roleStage.show();
    }
    
    private void showGameUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        tradeButton = new Button("🤝 Trade");
        tradeButton.setStyle("-fx-font-size: 14px; -fx-background-color: #9C27B0; -fx-text-fill: white; -fx-min-width: 150px;");
        tradeButton.setOnAction(e -> showPlayerTradeMenu());
        tradeButton.setDisable(true);

        // Menu bar
        MenuBar menuBar = new MenuBar();
        Menu gameMenu = new Menu("Game");
        MenuItem saveItem = new MenuItem("Save Game");
        saveItem.setOnAction(e -> saveGame());
        MenuItem loadItem = new MenuItem("Load Game");
        loadItem.setOnAction(e -> loadGame());
        MenuItem exitItem = new MenuItem("Exit to Menu");
        exitItem.setOnAction(e -> showMainMenu());
        gameMenu.getItems().addAll(saveItem, loadItem, exitItem);
        
        Menu helpMenu = new Menu("Help");
        MenuItem rulesItem = new MenuItem("Game Rules");
        rulesItem.setOnAction(e -> showRules());
        helpMenu.getItems().add(rulesItem);
        
        menuBar.getMenus().addAll(gameMenu, helpMenu);
        root.setTop(menuBar);
        
        // Map grid
        mapGrid = new GridPane();
        mapGrid.setHgap(5);
        mapGrid.setVgap(5);
        mapGrid.setAlignment(Pos.CENTER);
        mapGrid.setPadding(new javafx.geometry.Insets(20));
        updateMapGrid();
        
        // Right panel
        rightPanel = new VBox(10);
        rightPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 15px;");
        rightPanel.setPrefWidth(350);
        
        currentPlayerLabel = new Label("Current: " + game.getPlayers().get(game.getCurrentPlayerIndex()).getName());
        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        marketPricesLabel = new Label();
        updateMarketDisplay();
        
        playersResourcesBox = new VBox(5);
        updateResourcesDisplay();
        
        eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setPrefHeight(200);
        eventLogArea.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: #eeeeee;");
        updateEventLog();
        
        // Buttons
        rollButton = new Button("🎲");
        rollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 150px;");
        rollButton.setOnAction(e -> rollDice());
        
        buildMVPButton = new Button("🏗️ MVP");
        buildMVPButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 150px;");
        buildMVPButton.setOnAction(e -> showBuildMVPMenu());
        buildMVPButton.setDisable(true);
        
        upgradeButton = new Button("🦄 Unicorn");
        upgradeButton.setStyle("-fx-font-size: 14px; -fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 150px;");
        upgradeButton.setOnAction(e -> showUpgradeMenu());
        upgradeButton.setDisable(true);
        
        buildPartnershipButton = new Button("🤝 Partnership");
        buildPartnershipButton.setStyle("-fx-font-size: 14px; -fx-background-color: #9C27B0; -fx-text-fill: white; -fx-min-width: 150px;");
        buildPartnershipButton.setOnAction(e -> showBuildPartnershipMenu());
        buildPartnershipButton.setDisable(true);
        
        marketButton = new Button("💰 Market");
        marketButton.setStyle("-fx-font-size: 14px; -fx-background-color: #009688; -fx-text-fill: white; -fx-min-width: 150px;");
        marketButton.setOnAction(e -> showMarketMenu());
        marketButton.setDisable(true);
        

        endTurnButton = new Button("⏭️ Next");
        endTurnButton.setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 150px;");
        endTurnButton.setOnAction(e -> endTurn());
        endTurnButton.setDisable(true);

        HBox buttonBox = new HBox(10, rollButton, buildMVPButton, upgradeButton, buildPartnershipButton, marketButton, tradeButton, endTurnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        rightPanel.getChildren().addAll(
            currentPlayerLabel, 
            new Separator(),
            marketPricesLabel,
            new Separator(),
            new Label("Player Resources:"),
            playersResourcesBox,
            new Separator(),
            new Label("Event Log:"),
            eventLogArea,
            buttonBox
        );
        
        ScrollPane mapScroll = new ScrollPane(mapGrid);
        mapScroll.setFitToWidth(true);
        mapScroll.setFitToHeight(true);
        mapScroll.setStyle("-fx-background: #1a1a2e;");
        
        root.setCenter(mapScroll);
        root.setRight(rightPanel);
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley: The Tech Cartel");
        primaryStage.show();
        
        // Start auto-refresh for event log
        startEventLogUpdater();
    }
    
    private void startEventLogUpdater() {
        Thread updater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        updateEventLog();
                        updateResourcesDisplay();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updater.setDaemon(true);
        updater.start();
    }
    
    private void updateMapGrid() {
        mapGrid.getChildren().clear();
        GameMap gameMap = game.getGameMap();
        Sector[][] sectors = gameMap.getSectors();
        
        for (int row = 0; row < sectors.length; row++) {
            for (int col = 0; col < sectors[0].length; col++) {
                Sector sector = sectors[row][col];
                StackPane cell = createSectorCell(sector, row, col);
                mapGrid.add(cell, col, row);
            }
        }
    }
    
    private StackPane createSectorCell(Sector sector, int row, int col) {
    StackPane cell = new StackPane();
    cell.setPrefSize(100, 100);
    cell.setStyle("-fx-border-color: #333; -fx-border-width: 1px; -fx-border-radius: 5px;");
    
    Color bgColor = getSectorColor(sector.getType());
    Rectangle bg = new Rectangle(100, 100);
    bg.setFill(bgColor);
    bg.setArcWidth(10);
    bg.setArcHeight(10);
    
    // Inspector indicator
    if (game.getInspectorSector() == sector) {
        Rectangle inspectorBadge = new Rectangle(25, 25);
        inspectorBadge.setFill(Color.RED);
        inspectorBadge.setArcWidth(5);
        inspectorBadge.setArcHeight(5);
        Text inspectorText = new Text("🔍");
        inspectorText.setStyle("-fx-font-size: 12px; -fx-fill: WHITE;");
        StackPane inspectorPane = new StackPane(inspectorBadge, inspectorText);
        StackPane.setAlignment(inspectorPane, Pos.TOP_RIGHT);
        cell.getChildren().add(inspectorPane);
    }
    
    String displayName = sector.getType().display();
    String shortName = displayName.length() > 8 ? displayName.substring(0, 8) : displayName;
    Text typeText = new Text(shortName);
    typeText.setStyle("-fx-font-size: 9px; -fx-fill: WHITE; -fx-font-weight: bold;");
    
    Text activationText = new Text(sector.getType().isProductive() ? String.valueOf(sector.getActivationNumber()) : "⚖");
    activationText.setStyle("-fx-font-size: 18px; -fx-fill: WHITE; -fx-font-weight: bold;");
    
    // Get all vertices around this sector
    List<Vertex> verticesAround = game.getGameMap().verticesAround(sector);
    
    // Define the 4 corner positions (top-left, top-right, bottom-left, bottom-right)
    // Each corner has an (x, y) offset from the cell's top-left
    int[][] cornerPositions = {
        {5, 5},      // Top-left
        {85, 5},     // Top-right
        {5, 85},     // Bottom-left
        {85, 85}     // Bottom-right
    };
    
    // For each vertex, determine which corner it belongs to and create a circle
    for (Vertex v : verticesAround) {
        // Determine corner based on which sectors are adjacent
        // A vertex at top-left of this sector is also adjacent to sectors (row-1, col-1), (row-1, col), (row, col-1)
        boolean isTopLeft = false;
        boolean isTopRight = false;
        boolean isBottomLeft = false;
        boolean isBottomRight = false;
        
        // Get all adjacent sectors for this vertex
        List<Sector> adjSectors = v.getAdjacentSectors();
        
        // Check if this vertex is at the top-left corner of the current sector
        // Top-left corner vertices are adjacent to this sector, the one above, the one left, and the one above-left
        boolean hasAbove = false;
        boolean hasLeft = false;
        boolean hasAboveLeft = false;
        
        for (Sector s : adjSectors) {
            if (s.getRow() == row - 1 && s.getCol() == col) hasAbove = true;
            if (s.getRow() == row && s.getCol() == col - 1) hasLeft = true;
            if (s.getRow() == row - 1 && s.getCol() == col - 1) hasAboveLeft = true;
        }
        
        // Vertex is top-left if it's adjacent to this sector and has the above/left pattern
        if (adjSectors.contains(sector) && (hasAbove || hasLeft || hasAboveLeft)) {
            isTopLeft = true;
        }
        
        // Check top-right
        boolean hasAboveRight = false;
        boolean hasRight = false;
        for (Sector s : adjSectors) {
            if (s.getRow() == row - 1 && s.getCol() == col + 1) hasAboveRight = true;
            if (s.getRow() == row && s.getCol() == col + 1) hasRight = true;
        }
        if (adjSectors.contains(sector) && (hasAboveRight || hasRight)) {
            isTopRight = true;
        }
        
        // Check bottom-left
        boolean hasBelowLeft = false;
        boolean hasBelow = false;
        for (Sector s : adjSectors) {
            if (s.getRow() == row + 1 && s.getCol() == col - 1) hasBelowLeft = true;
            if (s.getRow() == row + 1 && s.getCol() == col) hasBelow = true;
        }
        if (adjSectors.contains(sector) && (hasBelowLeft || hasBelow)) {
            isBottomLeft = true;
        }
        
        // Check bottom-right
        boolean hasBelowRight = false;
        boolean hasRightBottom = false;
        for (Sector s : adjSectors) {
            if (s.getRow() == row + 1 && s.getCol() == col + 1) hasBelowRight = true;
            if (s.getRow() == row && s.getCol() == col + 1) hasRightBottom = true;
        }
        if (adjSectors.contains(sector) && (hasBelowRight || hasRightBottom)) {
            isBottomRight = true;
        }
        
        // Also check if this vertex is in the middle of 4 sectors (standard case)
        // Standard corner detection: vertex is at intersection of 4 sectors
        boolean hasThis = adjSectors.contains(sector);
        boolean hasRightSector = false;
        boolean hasDownSector = false;
        boolean hasDownRightSector = false;
        
        for (Sector s : adjSectors) {
            if (s.getRow() == row && s.getCol() == col + 1) hasRightSector = true;
            if (s.getRow() == row + 1 && s.getCol() == col) hasDownSector = true;
            if (s.getRow() == row + 1 && s.getCol() == col + 1) hasDownRightSector = true;
        }
        
        if (hasThis && hasRightSector && hasDownSector && hasDownRightSector) {
            // This vertex is at top-left corner of this sector
            isTopLeft = true;
        }
        
        // Simplified detection: use vertex ID modulo pattern as fallback
        if (!isTopLeft && !isTopRight && !isBottomLeft && !isBottomRight) {
            int vertexId = v.getId();
            if (vertexId % 4 == 0) isTopLeft = true;
            else if (vertexId % 4 == 1) isTopRight = true;
            else if (vertexId % 4 == 2) isBottomLeft = true;
            else isBottomRight = true;
        }
        
        // Create the vertex circle
        Circle vertexCircle = new Circle(12);
        vertexCircle.setFill(Color.WHITE);
        vertexCircle.setStroke(Color.BLACK);
        vertexCircle.setStrokeWidth(1.5);
        
        // Add glow effect if occupied
        if (v.isOccupied()) {
            vertexCircle.setFill(Color.GOLD);
            vertexCircle.setStroke(Color.ORANGE);
            vertexCircle.setStrokeWidth(2);
        }
        
        // Vertex index label (BLACK text on white circle)
        Text vertexIndex = new Text(String.valueOf(v.getId()));
        vertexIndex.setStyle("-fx-font-size: 10px; -fx-fill: BLACK; -fx-font-weight: bold;");
        
        StackPane vertexPane = new StackPane(vertexCircle, vertexIndex);
        
        // Position the vertex at the correct corner
        if (isTopLeft) {
            vertexPane.setLayoutX(cornerPositions[0][0] - 12);
            vertexPane.setLayoutY(cornerPositions[0][1] - 12);
        } else if (isTopRight) {
            vertexPane.setLayoutX(cornerPositions[1][0] - 12);
            vertexPane.setLayoutY(cornerPositions[1][1] - 12);
        } else if (isBottomLeft) {
            vertexPane.setLayoutX(cornerPositions[2][0] - 12);
            vertexPane.setLayoutY(cornerPositions[2][1] - 12);
        } else if (isBottomRight) {
            vertexPane.setLayoutX(cornerPositions[3][0] - 12);
            vertexPane.setLayoutY(cornerPositions[3][1] - 12);
        } else {
            // Default position - top-left
            vertexPane.setLayoutX(cornerPositions[0][0] - 12);
            vertexPane.setLayoutY(cornerPositions[0][1] - 12);
        }
        
        // Add click handler for building MVP on vertex
        final Vertex selectedVertex = v;
        vertexPane.setOnMouseClicked(event -> {
            if (!v.isOccupied() && !rollButton.isDisable() && buildMVPButton.isDisable()) {
                try {
                    game.buildMVP(game.getCurrentPlayerIndex(), selectedVertex.getId());
                    updateMapGrid();
                    updateResourcesDisplay();
                    updateEventLog();
                    showInfo("MVP built at vertex " + selectedVertex.getId());
                } catch (Exception ex) {
                    showError("Cannot build MVP: " + ex.getMessage());
                }
            } else if (v.isOccupied()) {
                showInfo("Vertex " + v.getId() + " is already occupied by " + 
                    (v.getStructure() instanceof Unicorn ? "Unicorn" : "MVP"));
            }
        });
        
        // Add hover effect
        vertexPane.setOnMouseEntered(event -> {
            if (!v.isOccupied()) {
                vertexCircle.setFill(Color.LIGHTYELLOW);
                vertexCircle.setStroke(Color.GOLD);
            }
        });
        vertexPane.setOnMouseExited(event -> {
            if (!v.isOccupied()) {
                vertexCircle.setFill(Color.WHITE);
                vertexCircle.setStroke(Color.BLACK);
            } else {
                vertexCircle.setFill(Color.GOLD);
                vertexCircle.setStroke(Color.ORANGE);
            }
        });
        
        cell.getChildren().add(vertexPane);
    }
    
    VBox labels = new VBox(5, activationText, typeText);
    labels.setAlignment(Pos.CENTER);
    
    cell.getChildren().addAll(bg, labels);
    return cell;
}
    
    private Color getSectorColor(SectorType type) {
        switch (type) {
            case HUB_AI: return Color.web("#443199");
            case DISTRICT_FINTECH: return Color.web("#111844");
            case CAMPUS_CLOUD: return Color.web("#C13383");
            case QUARTER_IP: return Color.web("#792CA2");
            case VALLEY_DATA: return Color.web("#E05454");
            case ZONE_REGULATORY: return Color.web("#555555");
            default: return Color.GRAY;
        }
    }
    
    private void updateMarketDisplay() {
        StringBuilder sb = new StringBuilder("💰 Market Prices:\n");
        for (Resource r : Resource.values()) {
            sb.append("  ").append(r.display()).append(": ").append(game.getMarket().priceOf(r)).append("\n");
        }
        marketPricesLabel.setText(sb.toString());
        marketPricesLabel.setStyle("-fx-text-fill: #eeeeee;");
    }
    
    private void updateResourcesDisplay() {
        playersResourcesBox.getChildren().clear();
        for (Player player : game.getPlayers()) {
            VBox playerBox = new VBox(3);
            playerBox.setStyle("-fx-background-color: #0f3460; -fx-padding: 5px; -fx-border-radius: 5px;");
            
            boolean isCurrent = player.getId() == game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            String style = isCurrent ? "-fx-text-fill: #ffd700; -fx-font-weight: bold;" : "-fx-text-fill: #eeeeee;";
            
            Label nameLabel = new Label(player.getName() + " (Score: " + player.computeScore() + ")");
            nameLabel.setStyle(style);
            
            HBox resourcesBox = new HBox(5);
            for (Resource r : Resource.values()) {
                int amount = player.getResource(r);
                if (amount > 0) {
                    String shortName = r.display().length() > 3 ? r.display().substring(0, 3) : r.display();
                    Label resLabel = new Label(shortName + ":" + amount);
                    resLabel.setStyle("-fx-background-color: " + getResourceColor(r) + "; -fx-padding: 2px 5px; -fx-border-radius: 3px; -fx-text-fill: white; -fx-font-size: 10px;");
                    resourcesBox.getChildren().add(resLabel);
                }
            }
            
            Label totalLabel = new Label("Total: " + player.totalCards());
            totalLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 10px;");
            
            playerBox.getChildren().addAll(nameLabel, resourcesBox, totalLabel);
            playersResourcesBox.getChildren().add(playerBox);
        }
    }
    
    private String getResourceColor(Resource r) {
        switch (r) {
            case TALENT: return "#443199";
            case CAPITAL: return "#111844";
            case CLOUD: return "#C13383";
            case DATA: return "#E05454";
            case PATENT: return "#792CA2";
            default: return "#555";
        }
    }
    
    private void updateEventLog() {
        StringBuilder sb = new StringBuilder();
        List<String> log = game.getEventLog();
        int start = Math.max(0, log.size() - 20);
        for (int i = start; i < log.size(); i++) {
            sb.append("• ").append(log.get(i)).append("\n");
        }
        eventLogArea.setText(sb.toString());
    }
    
    private void rollDice() {
        try {
            int roll = game.rollDice();
            updateMapGrid();
            updateMarketDisplay();
            updateResourcesDisplay();
            updateEventLog();
            
            // Check if crisis requires tax payment
            if (roll == GameConstants.CRISIS_DICE_SUM) {
                handleCrisisTax();
            } else {
                // Enable action buttons after roll
                enableActionButtons(true);
                rollButton.setDisable(true);
            }
            
        } catch (Exception ex) {
            showError("Roll failed: " + ex.getMessage());
        }
    }
    
    private void handleCrisisTax() {
        waitingForCrisisPayment = true;
        currentCrisisPlayerIndex = 0;
        processNextCrisisPayment();
    }
    
    private void processNextCrisisPayment() {
        if (currentCrisisPlayerIndex >= game.getPlayers().size()) {
            // All taxes paid, now place inspector
            waitingForCrisisPayment = false;
            showInspectorPlacement();
            return;
        }
        
        Player player = game.getPlayers().get(currentCrisisPlayerIndex);
        int pendingTax = player.getPendingTax();
        
        if (pendingTax > 0) {
            showTaxPaymentDialog(player, pendingTax);
        } else {
            currentCrisisPlayerIndex++;
            processNextCrisisPayment();
        }
    }
    
    private void showTaxPaymentDialog(Player player, int amount) {
        Stage taxStage = new Stage();
        VBox taxBox = new VBox(10);
        taxBox.setAlignment(Pos.CENTER);
        taxBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Label title = new Label(player.getName() + " - Pay Tax");
        title.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 18px;");
        
        Label info = new Label("You must return " + amount + " cards to the bank.");
        info.setStyle("-fx-text-fill: #eeeeee;");
        
        // Create spinners for each resource type
        Map<Resource, Integer> toReturn = new EnumMap<>(Resource.class);
        VBox resourcesBox = new VBox(5);
        
        for (Resource r : Resource.values()) {
            int available = player.getResource(r);
            if (available > 0) {
                HBox row = new HBox(10);
                Label resLabel = new Label(r.display() + " (" + available + "):");
                resLabel.setStyle("-fx-text-fill: #eeeeee;");
                Spinner<Integer> spinner = new Spinner<>(0, available, 0);
                spinner.setEditable(true);
                row.getChildren().addAll(resLabel, spinner);
                resourcesBox.getChildren().add(row);
                
                final Resource resource = r;
                spinner.valueProperty().addListener((obs, old, val) -> {
                    toReturn.put(resource, val);
                });
            }
        }
        
        Button confirmBtn = new Button("Pay Tax");
        confirmBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        confirmBtn.setOnAction(e -> {
            int total = toReturn.values().stream().mapToInt(Integer::intValue).sum();
            if (total == amount) {
                try {
                    game.applyCrisisTax(player.getId(), toReturn);
                    taxStage.close();
                    currentCrisisPlayerIndex++;
                    processNextCrisisPayment();
                    updateResourcesDisplay();
                    updateEventLog();
                } catch (Exception ex) {
                    showError("Failed to pay tax: " + ex.getMessage());
                }
            } else {
                showError("Must return exactly " + amount + " cards");
            }
        });
        
        taxBox.getChildren().addAll(title, info, resourcesBox, confirmBtn);
        Scene scene = new Scene(taxBox, 400, 400);
        taxStage.setScene(scene);
        taxStage.showAndWait();
    }
    
    private void showInspectorPlacement() {
        Stage inspectorStage = new Stage();
        VBox inspectorBox = new VBox(10);
        inspectorBox.setAlignment(Pos.CENTER);
        inspectorBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Label title = new Label("Place the Inspector");
        title.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 18px;");
        
        Label info = new Label("Click on a sector to place the inspector.");
        info.setStyle("-fx-text-fill: #eeeeee;");
        
        // Create a mini-map for inspector placement
        GridPane miniMap = new GridPane();
        miniMap.setHgap(2);
        miniMap.setVgap(2);
        
        Sector[][] sectors = game.getGameMap().getSectors();
        for (int row = 0; row < sectors.length; row++) {
            for (int col = 0; col < sectors[0].length; col++) {
                Sector sector = sectors[row][col];
                StackPane cell = new StackPane();
                cell.setPrefSize(50, 50);
                cell.setStyle("-fx-border-color: #333; -fx-border-width: 1px;");
                
                Color bgColor = getSectorColor(sector.getType());
                Rectangle bg = new Rectangle(50, 50);
                bg.setFill(bgColor);
                
                Text symbol = new Text(sector.getType().isProductive() ? String.valueOf(sector.getActivationNumber()) : "⚖");
                symbol.setStyle("-fx-fill: white;");
                
                cell.getChildren().addAll(bg, symbol);
                
                final int finalRow = row;
                final int finalCol = col;
                cell.setOnMouseClicked(e -> {
                    try {
                        game.placeInspector(game.getCurrentPlayerIndex(), finalRow, finalCol);
                        inspectorStage.close();
                        updateMapGrid();
                        updateEventLog();
                        enableActionButtons(true);
                        rollButton.setDisable(true);
                    } catch (Exception ex) {
                        showError("Cannot place inspector: " + ex.getMessage());
                    }
                });
                
                miniMap.add(cell, col, row);
            }
        }
        
        inspectorBox.getChildren().addAll(title, info, miniMap);
        Scene scene = new Scene(inspectorBox, 400, 500);
        inspectorStage.setScene(scene);
        inspectorStage.show();
    }
    
    private void showBuildMVPMenu() {
        List<Vertex> availableVertices = new ArrayList<>();
        for (Vertex v : game.getGameMap().getVertices().values()) {
            if (!v.isOccupied()) {
                boolean valid = true;
                for (Vertex other : game.getGameMap().getVertices().values()) {
                    if (other.isOccupied() && game.getGameMap().distanceInVertices(v.getId(), other.getId()) < 2) {
                        valid = false;
                        break;
                    }
                }
                if (valid) availableVertices.add(v);
            }
        }
        
        if (availableVertices.isEmpty()) {
            showError("No valid location for MVP!");
            return;
        }
        
        // Create readable labels for vertices
        Map<String, Vertex> vertexLabels = new LinkedHashMap<>();
        for (Vertex v : availableVertices) {
            // Find coordinates of this vertex (around which sectors)
            String coords = getVertexCoordinates(v);
            vertexLabels.put("Vertex " + v.getId() + " " + coords, v);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(vertexLabels.keySet().iterator().next(), vertexLabels.keySet());
        dialog.setTitle("Build MVP");
        dialog.setHeaderText("Select a vertex location\nCost: 1 Capital, 1 Talent, 1 Cloud, 1 Data");
        dialog.setContentText("Choose vertex:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Vertex vertex = vertexLabels.get(selected);
            try {
                game.buildMVP(game.getCurrentPlayerIndex(), vertex.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("MVP built successfully at " + selected);
            } catch (Exception ex) {
                showError("Cannot build MVP: " + ex.getMessage());
            }
        });
    }

    private void showUpgradeMenu() {
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        List<Vertex> upgradableVertices = new ArrayList<>();
        
        for (Vertex v : current.getOwnedVertices()) {
            if (v.getStructure() instanceof MVP) {
                upgradableVertices.add(v);
            }
        }
        
        if (upgradableVertices.isEmpty()) {
            showError("No MVP to upgrade!");
            return;
        }
        
        // Create readable labels for vertices
        Map<String, Vertex> vertexLabels = new LinkedHashMap<>();
        for (Vertex v : upgradableVertices) {
            String coords = getVertexCoordinates(v);
            vertexLabels.put("MVP at " + coords + " (Vertex " + v.getId() + ")", v);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(vertexLabels.keySet().iterator().next(), vertexLabels.keySet());
        dialog.setTitle("Upgrade to Unicorn");
        dialog.setHeaderText("Select an MVP to upgrade\nCost: 3 Data, 2 Cloud");
        dialog.setContentText("Choose MVP:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Vertex vertex = vertexLabels.get(selected);
            try {
                game.upgradeToUnicorn(game.getCurrentPlayerIndex(), vertex.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("MVP upgraded to Unicorn at " + selected);
            } catch (Exception ex) {
                showError("Cannot upgrade: " + ex.getMessage());
            }
        });
    }

    private void showBuildPartnershipMenu() {
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        List<Edge> availableEdges = new ArrayList<>();
        
        for (Edge e : game.getGameMap().getEdges().values()) {
            if (!e.hasPartnership()) {
                if (isConnectedToPlayer(current, e)) {
                    availableEdges.add(e);
                }
            }
        }
        
        if (availableEdges.isEmpty()) {
            showError("No valid location for Partnership!");
            return;
        }
        
        // Create readable labels for edges
        Map<String, Edge> edgeLabels = new LinkedHashMap<>();
        for (Edge e : availableEdges) {
            String coords = getEdgeCoordinates(e);
            edgeLabels.put("Edge " + e.getId() + " " + coords, e);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(edgeLabels.keySet().iterator().next(), edgeLabels.keySet());
        dialog.setTitle("Build Partnership");
        dialog.setHeaderText("Select an edge location\nCost: 1 Capital, 1 Patent");
        dialog.setContentText("Choose edge:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Edge edge = edgeLabels.get(selected);
            try {
                game.buildPartnership(game.getCurrentPlayerIndex(), edge.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("Partnership built successfully at " + selected);
            } catch (Exception ex) {
                showError("Cannot build Partnership: " + ex.getMessage());
            }
        });
    }

    // Helper method to get readable vertex coordinates
    private String getVertexCoordinates(Vertex vertex) {
        // Find the sectors around this vertex to determine position
        List<Sector> adjacent = vertex.getAdjacentSectors();
        if (adjacent.isEmpty()) return "(unknown location)";
        
        // Get min row and col from adjacent sectors
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        for (Sector s : adjacent) {
            minRow = Math.min(minRow, s.getRow());
            minCol = Math.min(minCol, s.getCol());
        }
        return "(between sectors " + minRow + "," + minCol + " and " + (minRow+1) + "," + (minCol+1) + ")";
    }

    // Helper method to get readable edge coordinates
    private String getEdgeCoordinates(Edge edge) {
        int vA = edge.getVertexA();
        int vB = edge.getVertexB();
        
        // Try to find sectors adjacent to this edge
        Vertex vertexA = game.getGameMap().getVertices().get(vA);
        if (vertexA == null) return "(unknown edge)";
        
        List<Sector> sectors = vertexA.getAdjacentSectors();
        if (!sectors.isEmpty()) {
            Sector s = sectors.get(0);
            return "(between sectors at " + s.getRow() + "," + s.getCol() + ")";
        }
        return "(edge between vertices " + vA + " and " + vB + ")";
    }
    
    private boolean isConnectedToPlayer(Player player, Edge edge) {
        int a = edge.getVertexA();
        int b = edge.getVertexB();
        
        for (Vertex v : player.getOwnedVertices()) {
            if (v.getId() == a || v.getId() == b) return true;
        }
        for (Edge e : player.getOwnedEdges()) {
            if (e.connects(a) || e.connects(b)) return true;
        }
        return false;
    }
    
    private void showMarketMenu() {
        Stage marketStage = new Stage();
        VBox marketBox = new VBox(10);
        marketBox.setAlignment(Pos.CENTER);
        marketBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        
        Label title = new Label("Dynamic Market");
        title.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 18px;");
        
        Label capitalLabel = new Label("Your Capital: " + current.getResource(Resource.CAPITAL));
        capitalLabel.setStyle("-fx-text-fill: #eeeeee;");
        
        for (Resource r : Resource.values()) {
            if (r != Resource.CAPITAL) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER);
                
                int price = game.getMarket().priceOf(r);
                int have = current.getResource(r);
                
                Label info = new Label(r.display() + " - Price: " + price + " (You have: " + have + ")");
                info.setStyle("-fx-text-fill: #eeeeee;");
                
                Button buyBtn = new Button("Buy");
                buyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                buyBtn.setOnAction(e -> {
                    try {
                        game.tradeWithMarket(game.getCurrentPlayerIndex(), Resource.CAPITAL, r);
                        marketStage.close();
                        updateResourcesDisplay();
                        updateMarketDisplay();
                        updateEventLog();
                    } catch (Exception ex) {
                        showError("Cannot buy: " + ex.getMessage());
                    }
                });
                
                Button sellBtn = new Button("Sell");
                sellBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                sellBtn.setOnAction(e -> {
                    try {
                        game.tradeWithMarket(game.getCurrentPlayerIndex(), r, Resource.CAPITAL);
                        marketStage.close();
                        updateResourcesDisplay();
                        updateMarketDisplay();
                        updateEventLog();
                    } catch (Exception ex) {
                        showError("Cannot sell: " + ex.getMessage());
                    }
                });
                
                row.getChildren().addAll(info, buyBtn, sellBtn);
                marketBox.getChildren().add(row);
            }
        }
        
        marketBox.getChildren().addAll(title, capitalLabel);
        
        Scene scene = new Scene(marketBox, 500, 400);
        marketStage.setScene(scene);
        marketStage.setTitle("Market");
        marketStage.show();
    }

    private void showPlayerTradeMenu() {
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        List<Player> others = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            if (p.getId() != current.getId()) others.add(p);
        }
        if (others.isEmpty()) {
            showError("No other players to trade with!");
            return;
        }

        Stage tradeStage = new Stage();
        tradeStage.setTitle("Propose Trade");

        VBox mainBox = new VBox(10);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");

        Label title = new Label("Propose a Trade");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");

        // Opponent selection
        Label opponentLabel = new Label("Select opponent:");
        opponentLabel.setStyle("-fx-text-fill: white;");
        ComboBox<Player> opponentCombo = new ComboBox<>();
        opponentCombo.getItems().addAll(others);
        opponentCombo.setPromptText("Choose player");
        opponentCombo.setStyle("-fx-text-fill: white; -fx-control-inner-background: #0f3460;");

        // Resources to give
        Label giveLabel = new Label("Resources YOU give:");
        giveLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        VBox giveBox = new VBox(5);
        Map<Resource, Integer> giveResources = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            int available = current.getResource(r);
            HBox row = new HBox(10);
            Label resLabel = new Label(r.display() + (available > 0 ? " (have " + available + "):" : " (have 0):"));
            resLabel.setStyle(available > 0 ? "-fx-text-fill: white;" : "-fx-text-fill: gray;");
            Spinner<Integer> spinner = new Spinner<>(0, available, 0);
            spinner.setEditable(true);
            if (available == 0) spinner.setDisable(true);
            row.getChildren().addAll(resLabel, spinner);
            giveBox.getChildren().add(row);
            final Resource resource = r;
            spinner.valueProperty().addListener((obs, old, val) -> giveResources.put(resource, val));
            giveResources.put(r, 0);
        }

        // Resources to receive
        Label receiveLabel = new Label("Resources YOU receive:");
        receiveLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        VBox receiveBox = new VBox(5);
        Map<Resource, Integer> receiveResources = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            HBox row = new HBox(10);
            Label resLabel = new Label(r.display() + ":");
            resLabel.setStyle("-fx-text-fill: white;");
            Spinner<Integer> spinner = new Spinner<>(0, 99, 0);
            spinner.setEditable(true);
            row.getChildren().addAll(resLabel, spinner);
            receiveBox.getChildren().add(row);
            final Resource resource = r;
            spinner.valueProperty().addListener((obs, old, val) -> receiveResources.put(resource, val));
            receiveResources.put(r, 0);
        }

        Button proposeBtn = new Button("Propose Trade");
        proposeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        proposeBtn.setOnAction(e -> {
            Player opponent = opponentCombo.getValue();
            if (opponent == null) {
                showError("Please select an opponent!");
                return;
            }
            // Check proposer has enough to give
            for (Map.Entry<Resource, Integer> entry : giveResources.entrySet()) {
                if (entry.getValue() > 0 && current.getResource(entry.getKey()) < entry.getValue()) {
                    showError("You don't have enough " + entry.getKey().display() + "!");
                    return;
                }
            }
            tradeStage.close();
            showTradeProposal(current, opponent, giveResources, receiveResources);
        });

        mainBox.getChildren().addAll(title, opponentLabel, opponentCombo,
                new Separator(), giveLabel, giveBox,
                new Separator(), receiveLabel, receiveBox,
                proposeBtn);

        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1a2e;");
        Scene scene = new Scene(scrollPane, 500, 600);
        tradeStage.setScene(scene);
        tradeStage.show();
    }

    private void showTradeProposal(Player proposer, Player opponent,
                                Map<Resource, Integer> give,
                                Map<Resource, Integer> receive) {
        Stage proposalStage = new Stage();
        proposalStage.setTitle("Trade Proposal");

        VBox mainBox = new VBox(10);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");

        Label title = new Label("Trade Proposal");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");

        Label info = new Label(proposer.getName() + " wants to trade with " + opponent.getName());
        info.setStyle("-fx-text-fill: white;");

        // What proposer gives
        Label giveTitle = new Label(proposer.getName() + " gives:");
        giveTitle.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        StringBuilder giveText = new StringBuilder();
        for (Map.Entry<Resource, Integer> entry : give.entrySet()) {
            if (entry.getValue() > 0) giveText.append("  • ").append(entry.getValue()).append(" ").append(entry.getKey().display()).append("\n");
        }
        Label giveLabel = new Label(giveText.length() > 0 ? giveText.toString() : "  Nothing");
        giveLabel.setStyle("-fx-text-fill: white;");

        // What proposer receives
        Label receiveTitle = new Label(proposer.getName() + " receives:");
        receiveTitle.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        StringBuilder receiveText = new StringBuilder();
        for (Map.Entry<Resource, Integer> entry : receive.entrySet()) {
            if (entry.getValue() > 0) receiveText.append("  • ").append(entry.getValue()).append(" ").append(entry.getKey().display()).append("\n");
        }
        Label receiveLabel = new Label(receiveText.length() > 0 ? receiveText.toString() : "  Nothing");
        receiveLabel.setStyle("-fx-text-fill: white;");

        // Check if opponent has enough resources to give
        boolean opponentCanGive = true;
        StringBuilder insufficient = new StringBuilder();
        for (Map.Entry<Resource, Integer> entry : receive.entrySet()) {
            if (entry.getValue() > 0 && opponent.getResource(entry.getKey()) < entry.getValue()) {
                opponentCanGive = false;
                insufficient.append("  • ").append(entry.getKey().display()).append(" (needs ").append(entry.getValue())
                        .append(", has ").append(opponent.getResource(entry.getKey())).append(")\n");
            }
        }

        if (!opponentCanGive) {
            Label errorLabel = new Label("Opponent does not have enough resources to give:\n" + insufficient.toString());
            errorLabel.setStyle("-fx-text-fill: #f44336;");
            mainBox.getChildren().addAll(title, info, errorLabel);
            Button okBtn = new Button("OK");
            okBtn.setOnAction(ev -> proposalStage.close());
            mainBox.getChildren().add(okBtn);
            Scene scene = new Scene(mainBox, 450, 400);
            proposalStage.setScene(scene);
            proposalStage.showAndWait();
            return;
        }

        // Ask opponent to accept or reject
        Label askLabel = new Label(opponent.getName() + ", do you accept this trade?");
        askLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Button acceptBtn = new Button("Accept");
        acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        acceptBtn.setOnAction(ev -> {
            // Execute trade
            for (Map.Entry<Resource, Integer> entry : give.entrySet()) {
                if (entry.getValue() > 0) {
                    proposer.removeResource(entry.getKey(), entry.getValue());
                    opponent.addResource(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<Resource, Integer> entry : receive.entrySet()) {
                if (entry.getValue() > 0) {
                    opponent.removeResource(entry.getKey(), entry.getValue());
                    proposer.addResource(entry.getKey(), entry.getValue());
                }
            }
            game.getEventLog().add(proposer.getName() + " traded with " + opponent.getName());
            updateResourcesDisplay();
            updateEventLog();
            showInfo("Trade completed!");
            proposalStage.close();
        });

        Button rejectBtn = new Button("Reject");
        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        rejectBtn.setOnAction(ev -> {
            showInfo("Trade rejected by " + opponent.getName());
            proposalStage.close();
        });

        HBox buttonBox = new HBox(20, acceptBtn, rejectBtn);
        buttonBox.setAlignment(Pos.CENTER);

        mainBox.getChildren().addAll(title, info, giveTitle, giveLabel, receiveTitle, receiveLabel, askLabel, buttonBox);

        Scene scene = new Scene(mainBox, 500, 500);
        proposalStage.setScene(scene);
        proposalStage.showAndWait();
    }
    
    private void enableActionButtons(boolean enabled) {
        buildMVPButton.setDisable(!enabled);
        upgradeButton.setDisable(!enabled);
        buildPartnershipButton.setDisable(!enabled);
        marketButton.setDisable(!enabled);
        tradeButton.setDisable(!enabled);
        endTurnButton.setDisable(!enabled);
    }
    
    private void endTurn() {
        try {
            game.endTurn();
            enableActionButtons(false);
            rollButton.setDisable(false);
            
            currentPlayerLabel.setText("Current: " + game.getPlayers().get(game.getCurrentPlayerIndex()).getName());
            updateResourcesDisplay();
            updateEventLog();
            
            if (game.isGameOver()) {
                showWinner();
            }
        } catch (Exception ex) {
            showError("End turn failed: " + ex.getMessage());
        }
    }
    
    private void saveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showSaveDialog(primaryStage);
        
        if (file != null) {
            try {
                game.saveGame(file.getAbsolutePath());
                showInfo("Game saved to " + file.getName());
            } catch (GameException ex) {
                showError("Save failed: " + ex.getMessage());
            }
        }
    }
    
    private void loadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                game = GameController.loadGame(file.getAbsolutePath());
                showGameUI();
            } catch (GameException ex) {
                showError("Load failed: " + ex.getMessage());
            }
        }
    }
    
    private void showRules() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Rules");
        alert.setHeaderText("Silicon Valley: The Tech Cartel");
        alert.setContentText(
            "🏆 WIN: First to 10 points!\n\n" +
            "⭐ SCORING:\n" +
            "  • MVP: 1 point\n" +
            "  • Unicorn: 2 points\n" +
            "  • Longest Partnership: 2 points\n" +
            "  • Founder Role: -1 point\n\n" +
            "🎲 GAMEPLAY:\n" +
            "  • Roll dice (2-12)\n" +
            "  • Matching sectors produce resources\n" +
            "  • Roll 7 = Crisis (Tax + Inspector)\n\n" +
            "🏗️ BUILD:\n" +
            "  • MVP: 1 each of Capital, Talent, Cloud, Data\n" +
            "  • Unicorn: 3 Data, 2 Cloud\n" +
            "  • Partnership: 1 Capital, 1 Patent"
        );
        alert.showAndWait();
    }
    
    private void showWinner() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("🏆 " + game.getWinner().getName() + " wins! 🏆");
        alert.setContentText("Final Score: " + game.getWinner().computeScore() + " points");
        alert.showAndWait();
        showMainMenu();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}