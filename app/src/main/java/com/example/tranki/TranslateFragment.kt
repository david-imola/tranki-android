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


class TranslateFragment() : Fragment(), Parcelable {
    private var tabPosition: Int = -1
    private var decks: ArrayList<String> = ArrayList<String>()

    private lateinit var credJson: String
    private var translationList: ArrayList<String> = ArrayList<String>()
    private lateinit var translationListView: ListView
    private var prefs: Preferences.Preferences? = null
    private var sourceLang = ""
    private var destLang = ""
    private var selectedDeck = -1

    private var translate: Translate = Translate()


    constructor(tabPosition: Int, sourceLang: String, destLang: String) : this() {
        this.tabPosition = tabPosition
        this.sourceLang = sourceLang
        this.destLang = destLang
    }

    constructor(tabPosition: Int) : this() {
        this.tabPosition = tabPosition
    }


    constructor(
        tabPosition: Int,
        decks: ArrayList<String>,
        translationList: ArrayList<String>,
        sourceLang: String,
        destLang: String,
        selectedDeck: Int
    ): this() {
        this.tabPosition = tabPosition
        this.decks = decks  // Make a copy to ensure mutability if needed


        this.translationList = translationList
        this.sourceLang = sourceLang
        this.destLang = destLang
        this.selectedDeck = selectedDeck
    }



    override fun describeContents(): Int {
        return 0
    }


    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(tabPosition)
        dest.writeStringList(decks)

        dest.writeStringList(translationList)

        dest.writeString(sourceLang)
        dest.writeString(destLang)

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

        translate.init(sourceLang, destLang,
            {
                val toast = Toast.makeText(requireContext(), "model download successful!", Toast.LENGTH_SHORT) // in Activity
                toast.show()
            }, {
                val text = "model download failed. closing app. Reason: ${it.message}"
                val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_LONG) // in Activity
                toast.show()
                throw it
            }
        )

        arguments?.let {
            tabPosition = it.getInt(ARG_POSITION)
            if (it.containsKey(ARG_DECKS)) {
                decks = it.getStringArrayList(ARG_DECKS)!!

                translationList = it.getStringArrayList(ARG_TRANSLATIONLIST)!!

                sourceLang = it.getString(ARG_SOURCE_LANG)!!
                destLang = it.getString(ARG_DEST_LANG)!!
                selectedDeck = it.getInt(ARG_SELECTED_DECK)
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

        // deck list
        if (decks.isEmpty()) {
            getDecks()
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

        // translate button
        val tranButton: Button = view.findViewById(R.id.translate_button)
        tranButton.setOnClickListener {
            val inputText = view.findViewById<EditText>(R.id.trans_input).text.toString()
            val outputTV: TextView = view.findViewById(R.id.trans_output)

            translate.translate(
                inputText, {
                    outputTV.text = it
                },
                {
                    Log.e("TranslateFragment", "Translation error")
                    Log.e("TranslateFragment", it.message, it)
                    Toast.makeText(
                        requireContext(),
                        "Translation Error: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    ""
                }
            )

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
            val key = if (prefs!!.ankiKeyEnabled!!) prefs!!.ankiKey else null
            val message = try {
                Anki.sync(
                    prefs!!.ankiUrl!!,
                    key,
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
        override fun createFromParcel(source: Parcel): TranslateFragment {
            val tabPosition = source.readInt()

            val decks = ArrayList<String>()
            source.readStringList(decks)

            val translationList = ArrayList<String>()
            source.readStringList(translationList)

            val selectedSource = source.readString()!!
            val selectedTarget = source.readString()!!

            val selectedDeck = source.readInt()


            return TranslateFragment(tabPosition, decks, translationList, selectedSource, selectedTarget, selectedDeck)
        }

        override fun newArray(size: Int): Array<TranslateFragment?> {
            return arrayOfNulls(size)
        }

        const val ARG_POSITION = "position"
        const val ARG_DECKS = "decks"
        const val ARG_TRANSLATIONLIST = "translationList"
        const val ARG_SOURCE_LANG = "selectedSource"
        const val ARG_DEST_LANG = "selectedTarget"
        const val ARG_SELECTED_DECK = "selectedDeck"

        @JvmStatic
        fun newInstance(position: Int, sourceLang: String, destLang: String) =
            TranslateFragment(position, sourceLang, destLang).apply {
                arguments = Bundle().apply {
                    putInt(ARG_POSITION, position)
                    putString(ARG_SOURCE_LANG, sourceLang)
                    putString(ARG_DEST_LANG, destLang)
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

                    val translationList = ArrayList<String>()
                    parcel.readStringList(translationList)
                    putStringArrayList(ARG_TRANSLATIONLIST, translationList)

                    putString(ARG_SOURCE_LANG, parcel.readString())
                    putString(ARG_DEST_LANG, parcel.readString())

                    putInt(ARG_SELECTED_DECK, parcel.readInt())
                }
            }
    }



}