package com.inovance.elevatorcontrol.models;

import android.content.Context;

import com.inovance.elevatorcontrol.R;
import com.mobsandgeeks.adapters.InstantText;

import java.util.ArrayList;
import java.util.List;

/**
 * 内召 外召
 */

public class MoveInsideOutside {

    private String name;//内召或外召

    public static List<MoveInsideOutside> getInsideOutLists(Context context) {
        List<MoveInsideOutside> moveInsideOutsides = new ArrayList<MoveInsideOutside>();

        MoveInsideOutside moveInsideOutside = null;
        moveInsideOutside = new MoveInsideOutside();
        moveInsideOutside.setName(context.getResources().getString(R.string.call_inside_text));
        moveInsideOutsides.add(moveInsideOutside);

        moveInsideOutside = new MoveInsideOutside();
        moveInsideOutside.setName(context.getResources().getString(R.string.call_outside_text));
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
