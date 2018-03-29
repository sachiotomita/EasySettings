package com.hotmail.or_dvir.easysettings_dialogs.pojos;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hotmail.or_dvir.easysettings.enums.ESettingsTypes;
import com.hotmail.or_dvir.easysettings.pojos.EasySettings;
import com.hotmail.or_dvir.easysettings_dialogs.R;
import com.hotmail.or_dvir.easysettings_dialogs.events.ListSettingsValueChangedEvent;

import org.greenrobot.eventbus.EventBus;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * a settings object that when clicked, opens a list dialog.
 * see the methods in {@link Builder} for available options
 */
//todo if you'd like to save another type of value other than string,
//todo just change the generic type.
//todo e.g. ListSettingsObject extends DialogSettingsObject<ListSettingsObject.Builder, Float>
public class ListSettingsObject extends DialogSettingsObject<ListSettingsObject.Builder, String>
		implements Serializable
{
	//////////////////////////////////////////////////////////
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!!			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!!			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!!			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!!			//
	//WARNING!!! WARNING!!! WARNING!!! WARNING!!!			//
	//do NOT change this value!!! if you do, you will		//
	//break backwards compatibility!!!						//
	//use a value that is unlikely to be used				//
	//by the user											//
	public static final String DELIMITER = ",-,-,-,-";		//
	//////////////////////////////////////////////////////////

	//////////////////////////////////////////
	//mandatory variables					//
    private ArrayList<String> listItems;	//
    //////////////////////////////////////////

	private boolean isMultiChoice;
    private Integer[] selectedItemsIndices;

    private ListSettingsObject(Builder builder)
    {
        super(builder);
        this.listItems = builder.listItems;
        this.isMultiChoice = builder.isMultiChoice;

        //todo if you don't want to use the builder pattern,
		//todo you can also use a regular constructor
    }

	/**
	 * @return all the items of this list dialog
	 */
	public ArrayList<String> getListItems()
    {
        return listItems;
    }

	/**
	 * @return the indices of the selected items
	 */
	public Integer[] getSelectedItemsIndices()
    {
        return selectedItemsIndices;
    }

	/**
	 * @return true if this dialog is a multi choice list, false otherwise
	 */
	public boolean isMultiChoice()
	{
		return isMultiChoice;
	}

	/**
	 * convenience method for {@link ListSettingsObject#prepareValuesAsSingleString(String...)}
	 */
	public static String prepareValuesAsSingleString(ArrayList<String> values)
	{
		return prepareValuesAsSingleString(values.toArray(new String[values.size()]));
	}

	/**
	 * creates a single string separated by a delimiter to be
	 * saved in the apps' {@link SharedPreferences}.
	 * @param values the list of individual values to save.
	 *               NOTE: these values will be trimmed (" one " will become "one")
	 * @return the string to be saved in shared preferences, including the delimiter.
	 * e.g. if the selected items of this list dialog are "one" and "two",
	 * this method will return "one[delimiter]two"
	 */
	public static String prepareValuesAsSingleString(String... values)
	{
		StringBuilder sb = new StringBuilder();

		for (String str : values)
		{
			//must trim the value here because the user might have blank spaces.
			//this will help prevent bugs generated by an empty space at the
			//beginning/end of a string (which could be very hard to find!)
			sb.append(str.trim())
			  .append(DELIMITER);
		}

		//remove the last delimiter
		sb.delete(sb.lastIndexOf(DELIMITER), sb.length());

		return sb.toString();
	}

	@Override
    public int getLayout()
    {
        //todo in this case i am using the same layout as a basic settings object
        //todo which simply contains a title text view, and a summary text view
        //todo you can put your own custom layout here
        return R.layout.basic_settings_object;
    }

	/**
	 * a helper method to check whether the contents of itemsToCheck are either missing
	 * or existing from this dialogs' item list.
	 * this method is used as part of the data validity check
	 *
	 * @param itemsToCheck
	 * @param getExisting  if you wish to test whether itemsToCheck exist in this dialogs' items list
	 *                     pass true, to test if they don't exist pass false
	 * @return a list of all the the string in itemsToCheck that are missing/existing in this
	 * dialogs' items list
	 */
	private ArrayList<String> getMissingOrExistingValuesFromListItems(String[] itemsToCheck,
																	  boolean getExisting)
	{
		ArrayList<String> arrayToReturn = new ArrayList<>(itemsToCheck.length);

		boolean found;
		for (String strToCheck : itemsToCheck)
		{
			found = false;

			for(String listItem : listItems)
			{
				if(strToCheck.equalsIgnoreCase(listItem))
				{
					found = true;
					break;
				}
			}

			if (found == getExisting)
			{
				arrayToReturn.add(strToCheck);
			}
		}

		return arrayToReturn;
	}

	/**
	 *
	 * @param prefs this apps' {@link SharedPreferences} where the settings are saved
	 * @return a list of the individual strings that are currently saved in this {@link ListSettingsObject}
	 */
	private String[] getPreviouslySavedValues(SharedPreferences prefs)
	{
		String prevValuesAsSingleString = prefs.getString(getKey(), getDefaultValue());
		return prevValuesAsSingleString.split(DELIMITER);
	}

	/**
	 *
	 * @param context a context used to get the apps' {@link SharedPreferences}
	 * @return a list of the individual strings that are currently saved in this {@link ListSettingsObject}
	 */
	private String[] getPreviouslySavedValues(Context context)
	{
		SharedPreferences prefs = EasySettings.retrieveSettingsSharedPrefs(context);
		return getPreviouslySavedValues(prefs);
	}

	/**
	 * @return the previously existing values separated by a delimiter, minus values
	 * that are no longer a part of this {@link ListSettingsObject}.
	 * e.g. imagine you have a list with values A, B, C, D and the user chooses A, B, and C.
	 * in the next version, for some reason, you removes option "A" from the list.
	 * now one of the previously saved values (A) in the SharedPreferences does not match
	 * any of the new list items. in this case this method will return "B{DELIMITER}C".
	 * NOTE: in case none of the previously saved values match the current list values, the default values
	 * are returned (separated by the delimiter)
	 * @throws UnsupportedOperationException if this {@link ListSettingsObject} contains less than 2 items
	 * @throws IllegalArgumentException if one of the default values provided to this {@link ListSettingsObject}
	 * does not exist in the list of items provided
	 */
	@Override
	public String checkDataValidity(Context context, SharedPreferences prefs)
			throws UnsupportedOperationException,
				   IllegalArgumentException
	{
		if(listItems.size() < 2)
		{
			throw new UnsupportedOperationException("ListSettingsObject with key \"" + getKey() + "\" " +
													"must contain at least 2 elements");
		}

		ArrayList<String> missingDefaultValues = getMissingOrExistingValuesFromListItems(getDefaultValue().split(DELIMITER),
																						 false);
		if (missingDefaultValues.size() > 0)
		{
			throw new IllegalArgumentException("all default values provided for ListSettingsObject " +
											   "with key \"" + getKey() + "\" " +
											   "must be included the specified list elements.\n" +
											   "missing default values were " + missingDefaultValues.toString() +
											   ", specified list elements were " + listItems.toString());
		}

		//consider this scenario:
		//the developer has a list dialog with the values "A, B, C, D", and the user sets it to A.
		//in the next version, the developer removes option "A" from the list.
		//now the previously saved value ("A") in the SharedPreferences does not match
		//any of the new list items
		String[] allPreviousValues = getPreviouslySavedValues(context);
		ArrayList<String> existingPreviousValues = getMissingOrExistingValuesFromListItems(allPreviousValues,
																						  true);
		String newStringToSave;
		if(existingPreviousValues.size() > 0)
		{
			String[] existingValuesAsArray = existingPreviousValues.toArray(new String[existingPreviousValues.size()]);
			newStringToSave = prepareValuesAsSingleString(existingValuesAsArray);
		}

		//no previously saved values still exist!
		//use default values
		else
		{
			newStringToSave = getDefaultValue();
		}

		//save the new string to sharedPreferences so that
		//next time it is being accessed, its' value will be valid
		setValueAndSaveSetting(context, newStringToSave);

		return newStringToSave;
	}

	/**
	 *
	 * @return if single choice list - the currently selected item.<br></br>
	 * if multi-choice list - the currently selected items, separated by a comma and a space
	 * e.g. if selected items are A B and C - this method returns "A, B, C"
	 * NOTE: the values are trimmed
	 */
	@Override
	public String getValueHumanReadable()
	{
		//single choice
		if(isMultiChoice() == false)
		{
			return listItems.get(selectedItemsIndices[0]);
		}

		//multi-choice
		else
		{
			String[] selectedItems = new String[selectedItemsIndices.length];

			for (int i = 0; i < selectedItems.length; i++)
			{
				selectedItems[i] = listItems.get(selectedItemsIndices[i]);
			}

			StringBuilder sb = new StringBuilder();
			String delimiter = ", ";

			for (String str : selectedItems)
			{
				//must trim the value here because the user might have blank spaces.
				//this will help prevent bugs generated by an empty space at the
				//beginning/end of a string (which could be very hard to find!)
				sb.append(str.trim())
				  .append(delimiter);
			}

			//remove the last delimiter
			sb.delete(sb.lastIndexOf(delimiter), sb.length());

			return sb.toString();
		}
	}

	/**
	 *
	 * @param context
	 * @return the indices in this {@link ListSettingsObject} that match the previously
	 * selected items
	 */
	private Integer[] getPreviouslySelectedIndices(Context context)
	{
		String[] previousValues = getPreviouslySavedValues(context);
		Integer[] indicesArray = new Integer[previousValues.length];

		for (int i = 0; i < previousValues.length; i++)
		{
			for (int j = 0; j < listItems.size(); j++)
			{
				//NOTE:
				//no need to use trim() here because this method is only called after
				//this object has already been generated and by this point,
				//all strings are already trimmed
				if(listItems.get(j).equalsIgnoreCase(previousValues[i]))
				{
					indicesArray[i] = j;
					break;
				}
			}
		}

		return indicesArray;
	}

    @Override
    public void initializeViews(View root)
    {
    	//IMPORTANT NOTE!!!
		//getValueHumanReadable is called from the super method, and
		//in this specific case it relies on the values of the selectedItemsIndices array.
		//therefore, the array needs to be initialized BEFORE calling the super method!!!
		selectedItemsIndices = getPreviouslySelectedIndices(root.getContext());

        //todo because we are using the R.layout.layout basic_settings_object,
        //todo we need to call the super method here to initialize the title and summary text views.
        //todo if you are making a completely custom layout here, no need to call the super method
        super.initializeViews(root);

        root.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            	showDialog(view);

            	//todo if you'd like, you can also send a custom event here to notify
				//todo the settings activity of the click.
				//todo NOTE: this specific line is only an example and is copy-pasted
				//todo from the class BasicSettingsObject.
				//todo you need to make your own custom event here
//				EventBus.getDefault().post(new BasicSettingsClickEvent(BasicSettingsObject.this));
            }
        });
    }

	/**
	 * creates and displays the actual dialog of this {@link ListSettingsObject}
	 * @param root the root view containing this {@link ListSettingsObject}
	 */
	private void showDialog(View root)
	{
		TextView tvSummary = null;

		Integer summaryId = getTextViewSummaryId();
		if(summaryId != null)
		{
			tvSummary = root.findViewById(summaryId);
		}

		final TextView finalTvSummary = tvSummary;

		MaterialDialog.Builder builder = getBasicMaterialDialogBuilder(root.getContext());
		builder.items(listItems);

		if(isMultiChoice)
		{
			builder.alwaysCallMultiChoiceCallback()
				   .itemsCallbackMultiChoice(selectedItemsIndices, new MaterialDialog.ListCallbackMultiChoice()
			{
				@Override
				public boolean onSelection(MaterialDialog dialog,
										   Integer[] which,
										   CharSequence[] text)
				{
					View btn = dialog.getActionButton(DialogAction.POSITIVE);

					if(which.length == 0)
					{
						btn.setEnabled(false);
					}

					else
					{
						btn.setEnabled(true);
					}

					return true;
				}
			})
			.onPositive(new MaterialDialog.SingleButtonCallback()
			{
				@Override
				public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
				{
					selectedItemsIndices = dialog.getSelectedIndices();

					//i know for a fact that this IS a multi choice list
					//and also that there is at least one item selected
					@SuppressWarnings("ConstantConditions")
					String[] selectedItems = new String[selectedItemsIndices.length];

					for (int i = 0; i < selectedItems.length; i++)
					{
						selectedItems[i] = listItems.get(selectedItemsIndices[i]);
					}

					String valueToSave = prepareValuesAsSingleString(selectedItems);
					//todo don't forget to set the new value!
					setValueAndSaveSetting(dialog.getContext(),
										   valueToSave);

					//NOTE:
					//this if statement MUST come AFTER setting the new value
					if(finalTvSummary != null &&
					   useValueAsSummary())
					{
						finalTvSummary.setText(getValueHumanReadable());
					}

					EventBus.getDefault().post(new ListSettingsValueChangedEvent(ListSettingsObject.this,
																				 valueToSave,
																				 selectedItems,
																				 selectedItemsIndices));
				}
			});
		}

		//single choice
		else
		{
			builder.itemsCallbackSingleChoice(selectedItemsIndices[0], new MaterialDialog.ListCallbackSingleChoice()
			{
				@Override
				public boolean onSelection(MaterialDialog dialog,
										   View itemView,
										   int position,
										   CharSequence text)
				{
					selectedItemsIndices[0] = position;

					String valueToSave = prepareValuesAsSingleString(listItems.get(position));
					//todo don't forget to set the new value!
					setValueAndSaveSetting(dialog.getContext(),
										   valueToSave);

					//NOTE:
					//this if statement MUST come AFTER setting the new value
					if(finalTvSummary != null &&
					   useValueAsSummary())
					{
						finalTvSummary.setText(getValueHumanReadable());
					}

					EventBus.getDefault().post(new ListSettingsValueChangedEvent(ListSettingsObject.this,
																				 valueToSave,
																				 new String[] {valueToSave},
																				 new Integer[] {selectedItemsIndices[0]}));
					return true;
				}
			});
		}

		builder.show();
	}

    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////

    public static class Builder extends DialogSettingsObject.Builder<Builder, String>
    {
        private ArrayList<String> listItems;
		private boolean isMultiChoice = false;

		/**
		 *
		 * @param key the key for this {@link ListSettingsObject}
		 *            to be saved in the apps' {@link SharedPreferences}
		 * @param title the title for this {@link ListSettingsObject}
		 * @param defaultValues the default values for this {@link ListSettingsObject}.<br></br>
		 *                      if single choice: must be a single string matching one of listItems.<br></br>
		 *                      if multi-choice: must be obtained using
		 *                      {@link ListSettingsObject#prepareValuesAsSingleString(String...)} or
		 *                      {@link ListSettingsObject#prepareValuesAsSingleString(ArrayList)}
		 * @param listItems the items this {@link ListSettingsObject} will display.
		 *                  NOTE: these items will be trimmed
		 * @param positiveBtnText the text to display for the positive button of the dialog
		 */
        public Builder(String key,
                       String title,
					   String defaultValues,
                       ArrayList<String> listItems,
					   String positiveBtnText)
        {
            //todo don't forget to pass your own id's here!
            super(key,
				  title,
				  defaultValues,
				  R.id.textView_basicSettingsObject_title,
				  R.id.textView_basicSettingsObject_summary,
				  ESettingsTypes.STRING,
				  R.id.imageView_basicSettingsObject_icon);

			setPositiveBtnText(positiveBtnText);

			this.listItems = new ArrayList<>(listItems.size());

            for(String str : listItems)
			{
				//must trim the value here because the user might have blank spaces.
				//this will help prevent bugs generated by an empty space at the
				//beginning/end of a string (which could be very hard to find!)
				this.listItems.add(str.trim());
			}
        }

		public ArrayList<String> getListItems()
		{
			return listItems;
		}

		public boolean isMultiChoice()
		{
			return isMultiChoice;
		}

		/**
		 * makes this {@link ListSettingsObject} multi-choice
		 * (default is single choice)
		 */
		public Builder setMultiChoice()
		{
			isMultiChoice = true;
			return this;
		}

        @Override
        public ListSettingsObject build()
        {
            return new ListSettingsObject(this);
        }
    }
}
