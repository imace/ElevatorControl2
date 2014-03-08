package com.kio.ElevatorControl.models;

import com.kio.ElevatorControl.R;
import com.mobsandgeeks.adapters.InstantText;

import java.util.ArrayList;
import java.util.List;

/**
 * 内招 外招
 */

public class MoveInsideOutside {

    private String name;//内招或外招

    public static List<MoveInsideOutside> getInsideOutLists() {
        List<MoveInsideOutside> moveInsideOutsides = new ArrayList<MoveInsideOutside>();

        MoveInsideOutside moveInsideOutside = null;
        moveInsideOutside = new MoveInsideOutside();
        moveInsideOutside.setName("内招");
        moveInsideOutsides.add(moveInsideOutside);

        moveInsideOutside = new MoveInsideOutside();
        moveInsideOutside.setName("外招");
        moveInsideOutsides.add(moveInsideOutside);
        return moveInsideOutsides;
    }

    @InstantText(viewId = R.id.text_transaction)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
