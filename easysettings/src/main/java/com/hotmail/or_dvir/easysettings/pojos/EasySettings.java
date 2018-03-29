package com.hotmail.or_dvir.easysettings.pojos;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.hotmail.or_dvir.easysettings.R;
import com.hotmail.or_dvir.easysettings.enums.ESettingsTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("PointlessBooleanExpression")
public class EasySettings
{
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! 			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! 			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! 			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! WARNING!!! 			//
	//do NOT change these values!!! they are being used as id's!!!										//
	private static final String SHARED_PREFERENCE_NAME = "com.hotmail.or_dvir.easysettings.settings";	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String customSharedPreferenceName = null;

	/**
	 * a helper method for creating an array of {@link SettingsObject}s for your app.
	 * @param settingsObjects
	 * @return
	 */
	public static ArrayList<SettingsObject> createSettingsArray(SettingsObject... settingsObjects)
	{
		ArrayList<SettingsObject> array = new ArrayList<>(settingsObjects.length);
		Collections.addAll(array, settingsObjects);

		return array;
	}

	/**
	 * WARNING!!! WARNING!!! WARNING!!!<br><br/>
	 * ONLY use this method if you are using this library on a pre-existing app which
	 * already has its own {@link SharedPreferences} to save its' settings,
	 * and you'd like to keep using it instead of this library's default {@link SharedPreferences} file
	 * @param name the name of the {@link SharedPreferences} to be used by this library
	 */
	public static void setCustomSharedPreferenceName(String name)
	{
		customSharedPreferenceName = name;
	}

	/**
	 * a helper method to retrieve an existing {@link SettingsObject} with the given key,
	 * inside the given settingsList
	 * @param key
	 * @param settingsList
	 * @return the {@link SettingsObject} with the given key, or null if no such {@link SettingsObject}
	 * was found in the given settingsList
	 */
	@Nullable
	public static SettingsObject findSettingsObject(String key, ArrayList<SettingsObject> settingsList)
	{
		for(SettingsObject obj : settingsList)
		{
			if(key.equals(obj.getKey()))
			{
				return obj;
			}
		}

		return null;
	}

	/**
	 * this method should be called as soon as possible from your apps main activity,
	 * and immediately after creating your settings (possibly using
	 * {@link EasySettings#createSettingsArray(SettingsObject[])}).<br></br>
	 * this method does the following:<br></br>
	 * 1) initializes all the given {@link SettingsObject}s inside the given settingsList.<br></br>
	 * 2) creates and saves the given settings inside the apps' {@link SharedPreferences}
	 * (unless the {@link SettingsObject} already exists).<br></br>
	 * 3) checks the validity of previously saved values, and if needed,
	 * saves the new value returned by {@link SettingsObject#checkDataValidity(Context, SharedPreferences)}
	 *
	 * @param context
	 * @param settingsList
	 */
	public static void initializeSettings(Context context,
										  ArrayList<SettingsObject> settingsList)
	{
		SharedPreferences prefs = retrieveSettingsSharedPrefs(context);
		SharedPreferences.Editor editor = prefs.edit();

		String key;
		Object defaultValue;
		for (SettingsObject settObj : settingsList)
		{
			key = settObj.getKey();
			defaultValue = settObj.getDefaultValue();

			//setting the value so it can be used by other methods
			settObj.setValue(defaultValue);

			//if the key does NOT exist, set it to DEFAULT VALUE.
			//if it DOES exist, don't do anything as to not override existing value
			//(see exceptions below this "if" statement)
			if (prefs.contains(key) == false)
			{
				ESettingsTypes type = settObj.getType();
				switch (type)
				{
					case VOID:
						//no actual value to save
						break;
					case BOOLEAN:
						editor.putBoolean(key, (boolean) defaultValue);
						break;
					case FLOAT:
						editor.putFloat(key, (float) defaultValue);
						break;
					case INTEGER:
						editor.putInt(key, (int) defaultValue);
						break;
					case LONG:
						editor.putLong(key, (long) defaultValue);
						break;
					case STRING:
						editor.putString(key, (String) defaultValue);
						break;
					case STRING_SET:
						//no need to check the cast because the defaultValue
						//is being checked in SettingsObject.Builder().
						//noinspection unchecked
						editor.putStringSet(key, (Set<String>) defaultValue);
						break;
					default:
						throw new IllegalArgumentException("did you forget to add \"" + type +
														   "\" to this switch statement?");
				}
			}

			//checking validity of previously saved values.
			//ONLY do this if the preference (key) actually exists in the SharedPreferences!
			//otherwise, prefs.getInt/Float/String...() will return the default value and we will end up
			//saving a value in the preferences which did not exist before!
			//if we enter the following else-if statement, than we know FOR SURE
			//that the settObj DOES exist in the shared preferences
			else
			{
				//checking data validity and setting the value
				//so it can be used by other methods
				settObj.setValue(settObj.checkDataValidity(context, prefs));
			}
		}

		editor.apply();
	}

	/**
	 * this method should called immediately after setContentView() in
	 * your settings activity (see sample app for an example).
	 * @param context a context to get the LAYOUT_INFLATER_SERVICE
	 * @param settingsActivityContainer the container of the settings
	 *                                  (most likely a vertical {@link LinearLayout})
	 * @param settingsList the list of {@link SettingsObject}s to use
	 */
	public static void inflateSettingsLayout(Context context,
											 ViewGroup settingsActivityContainer,
											 ArrayList<SettingsObject> settingsList)
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if(inflater == null)
		{
			throw new InflateException("inflateSettingsLayout: cannot retrieve LAYOUT_INFLATER_SERVICE");
		}

		Resources resources = context.getResources();
		SettingsObject settObj;
		for (int i = 0; i < settingsList.size(); i++)
		{
			settObj = settingsList.get(i);

			View individualSettingsContainer = inflater.inflate(settObj.getLayout(), settingsActivityContainer, false);
			settObj.initializeViews(individualSettingsContainer);
			settingsActivityContainer.addView(individualSettingsContainer);

			if(settObj.hasDivider())
			{
				int dividerHeight = (int) resources.getDimension(R.dimen.settings_divider_height);

				View divider = new View(context);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
																				 dividerHeight);
				divider.setBackgroundColor(Color.LTGRAY);
				divider.setLayoutParams(params);
				settingsActivityContainer.addView(divider);
			}

			if(settObj instanceof HeaderSettingsObject)
			{
				int settingsPadding = (int) resources.getDimension(R.dimen.settings_container_padding);
				int bottomPadding = individualSettingsContainer.getPaddingBottom();
				int topPadding = individualSettingsContainer.getPaddingTop();

				if(settingsList.get(i-1).hasDivider())
				{
					topPadding = settingsPadding;
				}

				if(settObj.hasDivider())
				{
					bottomPadding = settingsPadding;
				}

				individualSettingsContainer.setPadding(individualSettingsContainer.getPaddingLeft(),
													   topPadding,
													   individualSettingsContainer.getPaddingRight(),
													   bottomPadding);
			}
		}
	}

	/**
	 * a helper method to retrieve your apps' {@link SharedPreferences}
	 * where all the settings are being saved
	 * @param context
	 * @return your apps' {@link SharedPreferences}
	 * where all the settings are being saved
	 */
	public static SharedPreferences retrieveSettingsSharedPrefs(Context context)
	{
		String name = SHARED_PREFERENCE_NAME;

		if(customSharedPreferenceName != null)
		{
			name = customSharedPreferenceName;
		}

		return context.getSharedPreferences(name,
											Context.MODE_PRIVATE);
	}
}
