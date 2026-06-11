package packages;

public class Unicorn extends Company {
    public Unicorn(Player owner, Node position)
    {
        super(owner, position);
    }
    
    @Override
    public int getScoreValue()
    {
        return Constants.SCORE_UNICORN;
    }
}