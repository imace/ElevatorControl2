package com.kio.ElevatorControl.daos;

import java.util.List;

import com.kio.ElevatorControl.models.ParameterGroupSettings;
import net.tsz.afinal.FinalDb;
import android.content.Context;

public class ParameterGroupSettingsDao {
	private static final boolean DBDEBUG = true;

	public static List<ParameterGroupSettings> findAll(Context ctx) {
		// (android:label).db
		FinalDb db = FinalDb.create(ctx,
				ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
				DBDEBUG);
		List<ParameterGroupSettings> list = db
				.findAll(ParameterGroupSettings.class);
		return list;
	}

	public static ParameterGroupSettings findById(Context ctx, int id) {
		// (android:label).db
		FinalDb db = FinalDb.create(ctx,
				ctx.getString(ctx.getApplicationInfo().labelRes) + ".db",
				DBDEBUG);
		return db.findById(id, ParameterGroupSettings.class);
	}

}
