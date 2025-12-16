import zipfile
import xml.etree.ElementTree as ET
import os
import shutil
import base64
from datetime import datetime
from sqlalchemy.orm import Session
from . import models, database

class LegacyImporter:
    def __init__(self, db: Session, media_dir: str = "media"):
        self.db = db
        self.media_dir = media_dir
        if not os.path.exists(self.media_dir):
            os.makedirs(self.media_dir)

    def import_zip(self, zip_path: str):
        print(f"Opening zip: {zip_path}")
        with zipfile.ZipFile(zip_path, 'r') as z:
            # 1. Extract files
            for file_info in z.infolist():
                if file_info.filename.startswith("files/") and not file_info.filename.endswith("/"):
                    target_path = os.path.join(self.media_dir, os.path.basename(file_info.filename))
                    with open(target_path, "wb") as f_out:
                        f_out.write(z.read(file_info))

            # 2. Parse XML
            if "rdb-dump.xml" in z.namelist():
                with z.open("rdb-dump.xml") as f:
                    self.parse_and_import(f)
            else:
                print("No rdb-dump.xml found!")

    def parse_and_import(self, f):
        tree = ET.parse(f)
        root = tree.getroot()
        print(f"XML Root: {root.tag}")
        self.import_data(root)

    def import_data(self, root: ET.Element):
        users_cache = {} # username -> user_obj

        def get_or_create_user(username):
            if not username:
                return None
            if username in users_cache:
                return users_cache[username]

            # Use query on db to find existing
            user = self.db.query(models.User).filter_by(username=username).first()
            if not user:
                print(f"Creating user: {username}")
                user = models.User(username=username, role="user")
                self.db.add(user)
                self.db.commit() # Commit to get ID for sure
                self.db.refresh(user)
            users_cache[username] = user
            return user

        # 1. Fragments
        fragments = root.findall("fragment")
        print(f"Found {len(fragments)} fragments")
        for child in fragments:
            attrs = child.attrib
            creator_name = attrs.get("creator")
            user = get_or_create_user(creator_name)

            created_at = self.parse_timestamp(attrs.get("creation_datetime"))
            updated_at = self.parse_timestamp(attrs.get("update_datetime"))

            frag = models.Fragment(
                id=int(attrs.get("fragment_id")),
                title=attrs.get("title"),
                content=attrs.get("content"),
                created_at=created_at,
                updated_at=updated_at,
                creator_id=user.id if user else None,
                original_id=int(attrs.get("fragment_id"))
            )
            self.db.merge(frag)

        self.db.commit()

        # 2. Tags
        tags = root.findall("tag")
        print(f"Found {len(tags)} tags")
        for child in tags:
            attrs = child.attrib
            tag = models.Tag(
                id=int(attrs.get("tag_id")),
                name=attrs.get("tag_name"),
                description_fragment_id=int(attrs.get("fragment_id")) if attrs.get("fragment_id") else None
            )
            self.db.merge(tag)

        self.db.commit()

        # 3. Taggings
        taggings = root.findall("tagging")
        print(f"Found {len(taggings)} taggings")
        for child in taggings:
            attrs = child.attrib
            tagging = models.Tagging(
                tag_id=int(attrs.get("tag_id")),
                target_id=int(attrs.get("target_id")),
                target_type=int(attrs.get("target_type"))
            )
            self.db.merge(tagging)

        self.db.commit()

        # 4. Relations
        relations = root.findall("fragment_relation")
        print(f"Found {len(relations)} relations")
        for child in relations:
            attrs = child.attrib
            relation = models.FragmentRelation(
                parent_id=int(attrs.get("from_id")),
                child_id=int(attrs.get("to_id")),
                priority=int(attrs.get("priority")) if attrs.get("priority") else 0,
                is_bidirectional=attrs.get("two_way") == "true"
            )
            self.db.merge(relation)

        self.db.commit()

    def parse_timestamp(self, ts_str):
        if not ts_str: return None
        try:
            return datetime.strptime(ts_str.split('.')[0], "%Y-%m-%d %H:%M:%S")
        except:
            return None

if __name__ == "__main__":
    import sys
    # Ensure tables exist before import
    print("Ensuring database tables exist...")
    models.Base.metadata.create_all(bind=database.engine)

    if len(sys.argv) < 2:
        print("Usage: python -m app.importer <path_to_zip>")
        sys.exit(1)

    zip_path = sys.argv[1]
    db_gen = database.get_db()
    db = next(db_gen)
    importer = LegacyImporter(db)
    try:
        importer.import_zip(zip_path)
        print("Import completed successfully.")
    except Exception as e:
        print(f"Import failed: {e}")
    finally:
        db.close()
