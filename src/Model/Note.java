package Model;

public class Note {

    private final double x;
    private final double y;
    private final long appearTime;
    private final long duration;
    private final boolean trap;     // true = rombo rojo, clickear causa penalización
    private NoteState state;

    public Note(double x, double y, long appearTime, long duration, boolean trap) {
        this.x = x;
        this.y = y;
        this.appearTime = appearTime;
        this.duration   = duration;
        this.trap       = trap;
        this.state      = NoteState.FUTURE;
    }

    /** Constructor sin trampa — retrocompatibilidad */
    public Note(double x, double y, long appearTime, long duration) {
        this(x, y, appearTime, duration, false);
    }

    public double getX()          { return x; }
    public double getY()          { return y; }
    public long getAppearTime()   { return appearTime; }
    public long getDuration()     { return duration; }
    public boolean isTrap()       { return trap; }
    public NoteState getState()   { return state; }
    public void setState(NoteState state) { this.state = state; }

    public boolean shouldActivate(long gameTime) {
        return state == NoteState.FUTURE && gameTime >= appearTime;
    }

    public boolean isExpired(long gameTime) {
        return state == NoteState.ACTIVE && gameTime > appearTime + duration;
    }

    public boolean isInside(double clickX, double clickY, double radius) {
        double dx = clickX - x;
        double dy = clickY - y;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }
}