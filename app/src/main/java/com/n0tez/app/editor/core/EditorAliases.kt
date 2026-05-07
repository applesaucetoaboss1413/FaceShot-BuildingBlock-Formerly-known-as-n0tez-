package com.n0tez.app.editor.core

/**
 * Public editor-domain types, organized under stable packages for the upcoming multimedia editor suite.
 *
 * The current implementation is backed by the existing clean-room photo editor types to remain
 * backwards-compatible while we migrate UI and rendering layers into this module structure.
 */
typealias EditorState = com.n0tez.app.photoeditor.PhotoEditorState

typealias Adjustments = com.n0tez.app.photoeditor.Adjustments
typealias FilterPreset = com.n0tez.app.photoeditor.FilterPreset
typealias OverlayElement = com.n0tez.app.photoeditor.OverlayElement
typealias Stroke = com.n0tez.app.photoeditor.Stroke
typealias PointF = com.n0tez.app.photoeditor.PointF

