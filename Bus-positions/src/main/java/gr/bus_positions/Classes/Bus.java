package gr.bus_positions.Classes;
import java.io.Serializable;
/**
 * The Bus class is used to store the line number, the route code, the vehicle ID, the line name,
 * the line ID, the line's info and the route type.
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */
public class Bus implements Serializable {
    public static final long serialVersionUID = 972796977197385609L;
    private String lineNumber;
    private String routeCode;
    private String vehicleID;
    private String lineName;
    private String busLineID;
    private String info;
    private String routeType;

    public Bus(String lineNumber, String routeCode, String vehicleID, String lineName, String busLineID, String info, String routeType) {
        this.lineNumber = lineNumber;
        this.routeCode = routeCode;
        this.vehicleID = vehicleID;
        this.lineName = lineName;
        this.busLineID = busLineID;
        this.info = info;
        this.routeType = routeType;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public String getVehicleID() {
        return vehicleID;
    }

    public String getLineName() {
        return lineName;
    }

    public String getBusLineID() {
        return busLineID;
    }

    public String getInfo() {
        return info;
    }

    public String getRouteType() {
        return routeType;
    }
}