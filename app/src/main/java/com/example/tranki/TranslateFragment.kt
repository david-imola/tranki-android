package com.example.tranki

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast


class TranslateFragment() : Fragment(), Parcelable, View.OnClickListener {
    private var tabPosition: Int = -1
    private var decks: ArrayList<String> = ArrayList<String>()
    private var langs = ArrayList<SpinnerItem>()
    private lateinit var credJson: String
    private var translationList: ArrayList<String> = ArrayList<String>()
    private lateinit var translationListView: ListView
    private var prefs: Preferences.Preferences? = null
    private var selectedSource = ""
    private var selectedTarget = ""
    private var selectedDeck = -1


    constructor(tabPosition: Int) : this() {
        this.tabPosition = tabPosition
    }

    constructor(
        tabPosition: Int,
        decks: ArrayList<String>,
        labels: ArrayList<String>,
        values: ArrayList<String>,
        translationList: ArrayList<String>,
        selectedSource: String,
        selectedTarget: String,
        selectedDeck: Int
    ): this() {
        this.tabPosition = tabPosition
        this.decks = decks  // Make a copy to ensure mutability if needed

        if (labels.size == values.size) {
            for (i in labels.indices)
                langs.add(SpinnerItem(labels[i], values[i]))
        }

        this.translationList = translationList
        this.selectedSource = selectedSource
        this.selectedTarget = selectedTarget
        this.selectedDeck = selectedDeck
    }


    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(tabPosition)
        dest.writeStringList(decks)

        val labels = langs.map { it.label } as ArrayList<String>
        dest.writeStringList(labels)
        val values = langs.map { it.value } as ArrayList<String>
        dest.writeStringList(values)

        dest.writeStringList(translationList)

        dest.writeString(selectedSource)
        dest.writeString(selectedTarget)

        dest.writeInt(selectedDeck)
    }

    private class SpinnerItem(val label: String, val value: String) {
        @Override
        override fun toString(): String {
            return label
        }
    }

    private fun addItem(front: String, back: String) {
        val newItem = "$front\n$back"
        translationList.add(newItem)
        (translationListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun clearItems() {
        translationList.clear()
        (translationListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            tabPosition = it.getInt(ARG_POSITION)
            if (it.containsKey(ARG_DECKS)) {
                decks = it.getStringArrayList(ARG_DECKS)!!

                val labels = it.getStringArrayList(ARG_LABELS)!!
                val values = it.getStringArrayList(ARG_VALUES)!!
                if (labels.size == values.size) {
                    for (i in labels.indices)
                        langs.add(SpinnerItem(labels[i], values[i]))
                }
                translationList = it.getStringArrayList(ARG_TRANSLATIONLIST)!!

                selectedSource = it.getString(ARG_SELECTED_SOURCE)!!
                selectedTarget = it.getString(ARG_SELECTED_TARGET)!!
                selectedDeck = it.getInt(ARG_SELECTED_DECK)!!
            }
        }

        prefs = try {
            Preferences.get(requireActivity())
        } catch (e: Exception) {
            Log.e("TranslateFragment", "Error getting preferences")
            Log.e("TranslateFragment", e.message, e)
            null
        }
        if (prefs == null)
            return

        credJson = prefs!!.creds!!

        // deck list
        if (decks.isEmpty()) {
            getDecks()
        }

        // language list
        if (langs.isEmpty()) {
            try {
                val langPairs = Translate.getLanguages(credJson)
                for (p in langPairs)
                    langs.add(SpinnerItem(p.second, p.first))
            } catch (e: Exception) {
                Log.e("TranslateFragment", "Error getting language list")
                Log.e("TranslateFragment", e.message, e)
            }
        }


    }

    private fun getDecks() {
        decks = try {
            val keyNulled = if (prefs!!.ankiKeyEnabled!!) prefs!!.ankiKey else null
            Anki.decks(prefs!!.ankiUrl!!, keyNulled)!!
        } catch (e: Exception) {
            Log.e("TranslateFragment", "Error getting deck list")
            Log.e("TranslateFragment", e.message, e)
            ArrayList<String>()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translate, container, false)

        // source language spinner
        val sourceAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, langs)
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val sourceSpinner: Spinner = view.findViewById(R.id.sourcelang_spinner)
        sourceSpinner.adapter = sourceAdapter
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val mainActivity = activity as? MainActivity
                val text = langs[position].value
                mainActivity!!.setTabLeftText(tabPosition, text)
                selectedSource = text
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        if (selectedSource != "") {
            sourceSpinner.setSelection((langs.map { it.value } as ArrayList<String>).indexOf(selectedSource))
        }

        // target language spinner
        val targetAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, langs)
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val targetSpinner: Spinner = view.findViewById(R.id.targetlang_spinner)
        targetSpinner.adapter = targetAdapter
        targetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val mainActivity = activity as? MainActivity
                val text = langs[position].value
                mainActivity!!.setTabRightText(tabPosition, text)
                selectedTarget = text
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        if (selectedTarget != "") {
            targetSpinner.setSelection((langs.map { it.value } as ArrayList<String>).indexOf(selectedTarget))
        }

        // decks spinner
        val decksAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, decks)
        decksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val decksSpinner: Spinner = view.findViewById(R.id.deck_spinner)
        decksSpinner.adapter = decksAdapter
        decksSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDeck = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
        if (selectedDeck != -1) {
            decksSpinner.setSelection(selectedDeck)
        }

        // remove tab button
        val removeButton: Button = view.findViewById(R.id.remove_button)
        removeButton.setOnClickListener(this)

        // translate button
        val tranButton: Button = view.findViewById(R.id.translate_button)
        tranButton.setOnClickListener {
            val inputText = view.findViewById<EditText>(R.id.trans_input).text.toString()
            val sourceLang = sourceSpinner.selectedItem as SpinnerItem
            val targetLang = targetSpinner.selectedItem as SpinnerItem

            val output = try {
                Translate.translate(credJson, inputText, sourceLang.value, targetLang.value)
            } catch (e: Exception) {
                Log.e("TranslateFragment", "Translation error")
                Log.e("TranslateFragment", e.message, e)
                Toast.makeText(
                    requireContext(),
                    "Translation Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                ""
            }

            val outputTV: TextView = view.findViewById(R.id.trans_output)
            outputTV.text = output
        }

        // clear button
        val clearButton: Button = view.findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            val inputText: EditText = view.findViewById(R.id.trans_input)
            inputText.text.clear()

            val outputTV: TextView = view.findViewById(R.id.trans_output)
            outputTV.text = ""
        }

        // add button
        val addButton: Button = view.findViewById(R.id.add_button)
        addButton.setOnClickListener {
            val inputText = view.findViewById<EditText>(R.id.trans_input).text.toString()
            val outputText = view.findViewById<TextView>(R.id.trans_output).text.toString()
            addItem(inputText, outputText)
        }

        // swad button
        val swAdButton: Button = view.findViewById(R.id.swapadd_button)
        swAdButton.setOnClickListener {
            val inputText = view.findViewById<EditText>(R.id.trans_input).text.toString()
            val outputText = view.findViewById<TextView>(R.id.trans_output).text.toString()
            addItem(outputText, inputText)
        }

        // sync button
        val syncButton: Button = view.findViewById(R.id.sync_button)
        syncButton.setOnClickListener {
            val items = ArrayList<Pair<String, String>>()
            for (t in translationList) {
                val (front, back) = t.split("\n").take(2)
                items.add(Pair<String, String>(front, back))
            }

            val message = try {
                Anki.sync(
                    prefs!!.ankiUrl!!,
                    prefs!!.ankiKey,
                    decksSpinner.selectedItem as String,
                    items
                )
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown Error"
                Log.e("TranslateFragment", "Sync error")
                Log.e("TranslateFragment", msg, e)
                msg
            }

            if (message == null)
                clearItems()
            else
                Toast.makeText(requireContext(), "Sync error: $message", Toast.LENGTH_LONG).show()

        }

        // Sync decks button
        val decksButton: Button = view.findViewById(R.id.decks_button)
        decksButton.setOnClickListener {
            getDecks()
            decksAdapter.clear()
            decksAdapter.addAll(decks)
            decksAdapter.notifyDataSetChanged()
        }

        // list view of items to add to anki
        translationListView = view.findViewById(R.id.translation_list)
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            translationList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = super.getView(position, convertView, parent)
                val (text1, text2) = getItem(position)?.split("\n") ?: listOf("", "")
                itemView.findViewById<TextView>(android.R.id.text1).text = text1
                itemView.findViewById<TextView>(android.R.id.text2).text = text2
                return itemView
            }
        }
        translationListView.adapter = adapter
        translationListView.setOnItemLongClickListener { _, _, position, _ ->
            translationList.removeAt(position)
            adapter.notifyDataSetChanged()
            true
        }


        return view
    }
    fun setTabPosition(i: Int) {
        tabPosition = i
    }

    fun getTabPosition(): Int {
        return  tabPosition
    }


    companion object CREATOR: Parcelable.Creator<TranslateFragment?> {
        override fun createFromParcel(source: Parcel): TranslateFragment? {
            val tabPosition = source.readInt()

            val decks = ArrayList<String>()
            source.readStringList(decks)

            val labels = ArrayList<String>()
            source.readStringList(labels)

            val values = ArrayList<String>()
            source.readStringList(values)

            val translationList = ArrayList<String>()
            source.readStringList(translationList)

            val selectedSource = source.readString()!!
            val selectedTarget = source.readString()!!

            val selectedDeck = source.readInt()


            return TranslateFragment(tabPosition, decks, labels, values, translationList, selectedSource, selectedTarget, selectedDeck)
        }

        override fun newArray(size: Int): Array<TranslateFragment?> {
            return arrayOfNulls(size)
        }

        const val ARG_POSITION = "position"
        const val ARG_DECKS = "decks"
        const val ARG_LABELS = "labels"
        const val ARG_VALUES = "values"
        const val ARG_TRANSLATIONLIST = "translationList"
        const val ARG_SELECTED_SOURCE = "selectedSource"
        const val ARG_SELECTED_TARGET = "selectedTarget"
        const val ARG_SELECTED_DECK = "selectedDeck"

        @JvmStatic
        fun newInstance(position: Int) =
            TranslateFragment(position).apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSITION, position)
                }
            }

        @JvmStatic
        fun newInstance(parcel: Parcel) =
            TranslateFragment(parcel.readInt()).apply {
                arguments = Bundle().apply {

                    putInt(ARG_POSITION, tabPosition)

                    val decks = ArrayList<String>()
                    parcel.readStringList(decks)
                    putStringArrayList(ARG_DECKS, decks)

                    val labels = ArrayList<String>()
                    parcel.readStringList(labels)
                    putStringArrayList(ARG_LABELS, labels)

                    val values = ArrayList<String>()
                    parcel.readStringList(values)
                    putStringArrayList(ARG_VALUES, values)

                    val translationList = ArrayList<String>()
                    parcel.readStringList(translationList)
                    putStringArrayList(ARG_TRANSLATIONLIST, translationList)

                    putString(ARG_SELECTED_SOURCE, parcel.readString())
                    putString(ARG_SELECTED_TARGET, parcel.readString())

                    putInt(ARG_SELECTED_DECK, parcel.readInt())
                }
            }
    }

    override fun onClick(v: View?) {
        val mainActivity = activity as? MainActivity
        mainActivity!!.removeFragment(tabPosition)
    }


}