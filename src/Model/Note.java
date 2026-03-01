package Model;

public class Note {
    //por el momento la figura no se mueve
    private final double x;
    private final double y;
    private final long appearTime;
    private final long duration;
    private NoteState state;

    public Note(double x, double y, long appearTime, long duration) {
        this.x = x;
        this.y = y;
        this.appearTime = appearTime;
        this.duration = duration;
        this.state = NoteState.FUTURE;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getAppearTime() {
        return appearTime;
    }

    public long getDuration() {
        return duration;
    }

    public NoteState getState() {
        return state;
    }

    // la nota no fue activada y ya debería aparecer
    public boolean shouldActivate(long gameTime) {
        return state == NoteState.FUTURE && gameTime >= appearTime;
    }

    public boolean isExpired(long gameTime) {
        return state == NoteState.ACTIVE && gameTime > appearTime + duration;
    }

    public boolean isInside(double clickX, double clickY, double radius) {
        double dx = clickX - x;
        double dy = clickY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= radius;
    }

    public void setState(NoteState state){
        this.state = state;
    }




}
