package com.abjt.melnotes.listeners;

import com.abjt.melnotes.entities.Note;

public interface NotesListener {
    void OnNoteClicked(Note note, int position);
}
