package Model;

public class Note {

    private static final double MIN_X = 40;
    private static final double MAX_X = 760;
    private static final double MIN_Y = 40;
    private static final double MAX_Y = 480;

    private final double originX;
    private final double originY;
    private final long   appearTime;
    private final long   duration;
    private final NoteType type;

    // Solo para notas MOVING
    private final double vx;
    private final double vy;

    private NoteState state;

    // ------------------------------------------------------------------ //
    //  Constructores
    // ------------------------------------------------------------------ //

    public Note(double x, double y, long appearTime, long duration, NoteType type,
                double vx, double vy) {
        this.originX    = x;
        this.originY    = y;
        this.appearTime = appearTime;
        this.duration   = duration;
        this.type       = type;
        this.vx         = vx;
        this.vy         = vy;
        this.state      = NoteState.FUTURE;
    }

    public Note(double x, double y, long appearTime, long duration, NoteType type) {
        this(x, y, appearTime, duration, type, 0, 0);
    }

    /** Retrocompatibilidad con boolean trap */
    public Note(double x, double y, long appearTime, long duration, boolean trap) {
        this(x, y, appearTime, duration, trap ? NoteType.TRAP : NoteType.NORMAL, 0, 0);
    }

    /** Retrocompatibilidad sin tipo */
    public Note(double x, double y, long appearTime, long duration) {
        this(x, y, appearTime, duration, NoteType.NORMAL, 0, 0);
    }

    // ------------------------------------------------------------------ //
    //  Posicion actual
    // ------------------------------------------------------------------ //

    public double getX(long gameTime) {
        if (type != NoteType.MOVING) return originX;
        return bounced(originX, vx, gameTime - appearTime, MIN_X, MAX_X);
    }

    public double getY(long gameTime) {
        if (type != NoteType.MOVING) return originY;
        return bounced(originY, vy, gameTime - appearTime, MIN_Y, MAX_Y);
    }

    public double getX() { return originX; }
    public double getY() { return originY; }

    private static double bounced(double origin, double v, long elapsedNs,
                                  double min, double max) {
        double range    = max - min;
        double traveled = Math.abs(v) * (elapsedNs / 1_000_000_000.0);
        double offset   = origin - min;
        double total    = offset + traveled;
        double period   = 2.0 * range;
        double mod      = total % period;
        return mod <= range ? min + mod : max - (mod - range);
    }

    // ------------------------------------------------------------------ //
    //  Hit detection
    // ------------------------------------------------------------------ //

    public boolean isInside(double clickX, double clickY, double radius, long gameTime) {
        double cx = getX(gameTime);
        double cy = getY(gameTime);
        double dx = clickX - cx;
        double dy = clickY - cy;
        return Math.sqrt(dx * dx + dy * dy) <= radius;
    }

    public boolean isInside(double clickX, double clickY, double radius) {
        return isInside(clickX, clickY, radius, appearTime);
    }

    // ------------------------------------------------------------------ //
    //  Estado
    // ------------------------------------------------------------------ //

    public boolean shouldActivate(long gameTime) {
        return state == NoteState.FUTURE && gameTime >= appearTime;
    }

    public boolean isExpired(long gameTime) {
        return state == NoteState.ACTIVE && gameTime > appearTime + duration;
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //

    public long      getAppearTime() { return appearTime; }
    public long      getDuration()   { return duration; }
    public NoteType  getType()       { return type; }
    public boolean   isTrap()        { return type == NoteType.TRAP; }
    public boolean   isMoving()      { return type == NoteType.MOVING; }
    public NoteState getState()      { return state; }
    public void      setState(NoteState state) { this.state = state; }
}