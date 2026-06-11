package packages;

public class Sector
{
    private Node[] nodes; // 4: topLeft, topRight, bottomLeft, bottomRight
    private String type;
    private int diceNumber;
    private int x;
    private int y;
    private boolean hasAuditor;
    
    public Sector(int x, int y)
    {
        this.x = x;
        this.y = y;
        this.nodes = new Node[4];
        this.hasAuditor = false;
        
        nodes[0] = new Node(x, y);
        nodes[1] = new Node(x + Constants.SECTOR_SIZE, y);
        nodes[2] = new Node(x, y + Constants.SECTOR_SIZE);
        nodes[3] = new Node(x + Constants.SECTOR_SIZE, y + Constants.SECTOR_SIZE);
        
        this.type = Constants.SECTOR_TYPES[(int)(Math.random() * Constants.SECTOR_TYPES.length)];
        
        this.diceNumber = 2 + (int)(Math.random() * 11); // [2, 12]
    }
    
    public Sector(int x, int y, boolean isRegulatory)
    {
        this(x, y);
        if (isRegulatory)
        {
            this.type = Constants.SECTOR_TYPE_REGULATORY;
            this.diceNumber = 0;
        }
    }
    
    public Node[] getNodes()    { return nodes; }
    public String getType()     { return type; }
    public int getDiceNumber()  { return diceNumber; }
    public int getX()           { return x; }
    public int getY()           { return y; }

    public boolean hasAuditor() { return hasAuditor; }
    
    public void setAuditor(boolean auditor) { this.hasAuditor = auditor; }
    
    public String getResourceType()
    {
        for (String[] mapping : Constants.SECTOR_TO_RESOURCE)
            if (mapping[0].equals(type))
                return mapping[1];

        return null;
    }
    
    public boolean isResourceSector() { return !type.equals(Constants.SECTOR_TYPE_REGULATORY); }
    
    public boolean canProduce() { return isResourceSector() && !hasAuditor; }
}