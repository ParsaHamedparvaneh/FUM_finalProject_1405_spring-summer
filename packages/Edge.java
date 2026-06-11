package packages;

import java.util.*;;

public class Edge
{
    private Node node1;
    private Node node2;
    private Partnership partnership;
    private List<Sector> adjacentSectors;
    
    public Edge(Node node1, Node node2)
    {
        this.node1 = node1;
        this.node2 = node2;
        this.adjacentSectors = new ArrayList<Sector>();
        this.partnership = null;
    }
    
    public Node getNode1()              { return node1; }
    public Node getNode2()              { return node2; }
    public Partnership getPartnership() { return partnership; }
    
    public void setPartnership(Partnership partnership) { this.partnership = partnership; }
    
    public boolean hasPartnership()              { return partnership != null; }
    public void addAdjacentSector(Sector sector) { adjacentSectors.add(sector); }
}