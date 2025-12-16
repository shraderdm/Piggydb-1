# Piggydb Redesign Proposal

This document outlines the strategy for re-platforming Piggydb to a modern technology stack while maintaining data compatibility.

## 1. Technology Recommendation

### Language: **Python 3.11+**
**Why?**
-   **Knowledge Management Ecosystem:** Python is the dominant language in data science, text processing, and personal knowledge management (PKM) tools.
-   **Simplicity:** Matches the "standalone" ethos of Piggydb. Easy to read and maintain.
-   **SQLite Support:** First-class support in standard library and ORMs.
-   **Framework:** **FastAPI**.
    -   High performance.
    -   Automatic OpenAPI (Swagger) documentation.
    -   Easy to build a REST API to replace the server-side rendered pages.

### Database: **SQLite**
**Why?**
-   **Standalone:** Single file (`piggydb.db`), zero configuration. Matches the original "embedded H2" portability.
-   **Future Proof:** Easy to migrate to PostgreSQL later using tools like `pgloader` or ORM features.

### Frontend (Optional/Future): **React or Vue**
-   The new Python backend will serve a Single Page Application (SPA) or act as a headless API.

---

## 2. New Architecture

```mermaid
graph TD
    User[User / Browser] -->|HTTP/JSON| API[FastAPI App]
    API -->|ORM (SQLAlchemy)| DB[(SQLite Database)]
    API -->|File I/O| Files[File Storage /images]
    API -->|Import/Export| Dump[Legacy Zip Import]
```

### 2.1 Backend Structure (Python)
-   **`app/main.py`**: Entry point.
-   **`app/models`**: SQLAlchemy models mirroring the specification.
-   **`app/routers`**: API endpoints (`/fragments`, `/tags`, `/auth`).
-   **`app/services`**: Business logic (Tag hierarchy, Search).
-   **`app/utils/legacy_import.py`**: Logic to parse `rdb-dump.xml`.

---

## 3. Database Schema (SQLite)

We will modernize the schema naming slightly while keeping structure for compatibility.

```sql
CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR NOT NULL,
    password_hash VARCHAR,
    role VARCHAR
);

CREATE TABLE fragment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR,
    content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    creator_id INTEGER,
    -- File attachment columns
    file_name VARCHAR,
    file_path VARCHAR,
    file_type VARCHAR,
    file_size INTEGER,
    -- Legacy compatibility
    original_id INTEGER -- To store ID from H2 dump if needed
);

CREATE TABLE tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR UNIQUE NOT NULL,
    description_fragment_id INTEGER REFERENCES fragment(id)
);

CREATE TABLE tagging (
    tag_id INTEGER REFERENCES tag(id),
    target_id INTEGER, -- Poly-morphic ID
    target_type INTEGER, -- 1=Tag, 2=Fragment
    PRIMARY KEY (tag_id, target_id, target_type)
);

CREATE TABLE fragment_relation (
    parent_id INTEGER REFERENCES fragment(id),
    child_id INTEGER REFERENCES fragment(id),
    priority INTEGER DEFAULT 0,
    is_bidirectional BOOLEAN DEFAULT 0,
    PRIMARY KEY (parent_id, child_id)
);
```

---

## 4. Migration Strategy (The Critical Path)

To ensure backwards compatibility, we must implement an **Importer Tool**.

### 4.1 Input
The legacy `piggydb-YYYYMMDD.zip` file.

### 4.2 Algorithm
1.  **Extract Zip:** Unzip to a temp folder.
2.  **Parse XML:** Use Python's `xml.etree.ElementTree` to parse `rdb-dump.xml`.
3.  **Iterate & Insert:**
    -   **Tags:** Read `<tag>` elements -> Insert into SQLite `tag` table. Keep a map of `{old_id: new_id}` if IDs change (though we can force specific IDs in SQLite).
    -   **Fragments:** Read `<fragment>` elements -> Insert into `fragment` table. Handle base64 decoding if content is encoded, though usually it's plain text in DbUnit.
    -   **Relations:** Read `<fragment_relation>` and `<tagging>` -> Insert into join tables.
4.  **Move Files:** Copy files from `files/` directory to the new storage location.

### 4.3 Validation
-   Check row counts match.
-   Verify tag hierarchy is preserved.
-   Verify file attachments open correctly.

---

## 5. Implementation Roadmap

1.  **Setup:** Initialize Python project with FastAPI and SQLAlchemy.
2.  **Models:** Define the SQLite schema using Pydantic/SQLAlchemy models.
3.  **Importer:** Build the script to ingest legacy ZIP files.
4.  **API:** Implement CRUD endpoints.
5.  **UI:** (Out of scope for this phase, but API will be ready).
