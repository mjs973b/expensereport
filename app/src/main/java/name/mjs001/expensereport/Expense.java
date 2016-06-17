package name.mjs001.expensereport;

import android.os.Bundle;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Expense model.
 */
public class Expense {
    private static final NumberFormat fmt = NumberFormat.getCurrencyInstance();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static final String defaultDate = "1970-01-01";
    // fields corresponding to the expense table columns
    private int rowId;          // database row rowId
    private UserId userId;
    private CatId categoryId;
    private int cost;           // in cents
    private String description;
    private String date;        // yyyy-mm-dd

    public Expense() {
        rowId = -1;                     // invalid
        userId = new UserId(0);         // invalid
        categoryId = new CatId(0);      // invalid
        cost = 0;
        description = "";
        date = defaultDate;
    }

    public Expense(Expense oth) {
        rowId = oth.rowId;
        userId = oth.userId;
        categoryId = oth.categoryId;
        cost = oth.cost;
        description = oth.description;
        date = oth.date;
    }

    /** b might be null */
    public Expense(Bundle b) {
        this();
        if (b != null) {
            rowId = b.getInt("rowId", -1);
            int user_id = b.getInt("userId", 0);
            userId = new UserId(user_id);
            int catId = b.getInt("catId", 0);
            categoryId = new CatId(catId);
            cost = b.getInt("cost", 0);
            description = b.getString("desc", "");
            date = b.getString("date", defaultDate);
        }
    }

    public void copy(Expense oth) {
        rowId = oth.rowId;
        userId = oth.userId;
        categoryId = oth.categoryId;
        cost = oth.cost;
        description = oth.description;
        date = oth.date;
    }

    // field getters and setters

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userID) {
        this.userId = userID;
    }

    public CatId getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(CatId catId) {
        this.categoryId = catId;
    }

    /** in dollars (floating point) */
    public BigDecimal getCost() {
        return new BigDecimal(cost).movePointLeft(2);
    }

    /** plain number with formatting #.## */
    public String getCostAsString() {
        return getCost().toPlainString();
    }

    public int getCostAsCents() {
        return cost;
    }

    /** cost in cents */
    public void setCost(int cost) {
        this.cost = cost;
    }

    /** cost in dollars (floating point) */
    public void setCost(BigDecimal cost) {
        this.cost = cost.movePointRight(2).intValue();
    }

    public String getDescription() {
        return description;
    }

    /** double-quote char silently changed to underscore. */
    public void setDescription(String description) {
        // because of cvs export, double-quote char not permitted
        this.description = description.replace('"', '_');
    }

    public void setDate(String date) {
        this.date = date;
    }

    /** as yyyy-mm-dd */
    public String getDate() {
        return date;
    }

    public String getFormattedCost() {
        // fmt expects dollars
        return fmt.format(getCost());
    }

    public static String formatCost(int cents) {
        // fmt expects dollars
        return fmt.format(new BigDecimal(cents).movePointLeft(2));
    }

    @Override
    public String toString() {
        return getFormattedCost() + " " + description;
    }

    public void copyToBundle(Bundle b) {
        b.clear();
        b.putInt("rowId",   rowId);
        b.putInt("userId",  userId.toInt());
        b.putInt("catId",   categoryId.toInt());
        b.putInt("cost",    cost);
        b.putString("desc", description);
        b.putString("date", date);
    }

    // Return today's date as yyyy-mm-dd
    public static String today() {
        Calendar now = Calendar.getInstance();
        return sdf.format(now.getTime());
    }
}
