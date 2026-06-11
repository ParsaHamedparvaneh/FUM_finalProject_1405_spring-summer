package packages;

public abstract class Company
{
    protected Player owner;
    protected Node position;
    
    public Company(Player owner, Node position)
    {
        this.owner = owner;
        this.position = position;
    }
    
    public Player getOwner()  { return owner; }
    public Node getPosition() { return position; }
    public abstract int getScoreValue();
}