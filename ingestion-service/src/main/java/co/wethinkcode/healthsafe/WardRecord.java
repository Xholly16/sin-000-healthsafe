package co.wethinkcode.healthsafe;

public class WardRecord {
    public String wardId;
    public String wing;
    public String department;
    public Integer bedsAvailable;
    public String notes;

    public WardRecord(String wardId, String wing, String department, Integer bedsAvailable,String notes){
        this.wardId = wardId;
        this.wing = wing;
        this.department = department;
        this.bedsAvailable = bedsAvailable; // null if unknown/ invalid
        this.notes = notes;                 // explains what cleaning was done
    }
}
