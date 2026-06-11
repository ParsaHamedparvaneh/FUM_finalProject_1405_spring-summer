package packages;

public class MVP extends Company
{
    public MVP(Player owner, Node position)
    {
        super(owner, position);
    }
    
    @Override
    public int getScoreValue()
    {
        return Constants.SCORE_MVP;
    }
    
    public void upgradeToUnicorn() {}
}