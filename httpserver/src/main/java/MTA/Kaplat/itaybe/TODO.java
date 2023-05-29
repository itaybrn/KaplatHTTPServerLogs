package MTA.Kaplat.itaybe;

import java.util.Comparator;

public class TODO implements Comparable<TODO>
{

    public enum compareByOptions{
        ID,
        TITLE,
        DUE_DATE
    }
    public static int compareBy = compareByOptions.ID.ordinal();
    private static int IDGen = 1;
    public int ID;
    public String title;
    public String content;
    public long dueDate;
    public String status;


    TODO(String title, String content, long dueDate)
    {
        this.ID = TODO.IDGen;
        TODO.IDGen++;
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
        this.status = "PENDING";
    }

    public static int getIDGen() {
        return IDGen;
    }

    public String updateTODO(String newStatus)
    {
        String oldStatus = this.status;
        this.status = newStatus;
        return oldStatus;
    }

    @Override
    public int compareTo(TODO o) {
        switch (TODO.compareBy) {
            case 0: //ID
                return compareByID(o);
            case 1: //DUE_DATE
                return (int)compareByDueDate(o);
            case 2: //TITLE
                return compareByTitle(o);
            default:
                return 0;
        }
    }
    public int compareByID(TODO o) {
        return this.ID - o.ID;
    }

    public long compareByDueDate(TODO o) {
        return this.dueDate - o.dueDate;
    }

    public int compareByTitle(TODO o){
        return this.title.compareTo(o.title);
    }
}


