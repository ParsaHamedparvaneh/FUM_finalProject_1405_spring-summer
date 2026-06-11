package packages;

public class Partnership
{
    private Player owner;
    private Edge edge;
    
    public Partnership(Player owner, Edge edge)
    {
        this.owner = owner;
        this.edge = edge;
    }
    
    public Player getOwner() { return owner; }
    public Edge getEdge()    { return edge; }
}