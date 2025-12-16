from sqlalchemy import Boolean, Column, ForeignKey, Integer, String, Text, DateTime, func
from sqlalchemy.orm import relationship
from .database import Base

class User(Base):
    __tablename__ = "user"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String)
    role = Column(String)

class Fragment(Base):
    __tablename__ = "fragment"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=True)
    content = Column(Text, nullable=True)

    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())
    creator_id = Column(Integer, ForeignKey("user.id"), nullable=True)

    # File attachment columns
    file_name = Column(String, nullable=True)
    file_path = Column(String, nullable=True)
    file_type = Column(String, nullable=True)
    file_size = Column(Integer, nullable=True)

    # Legacy compatibility
    original_id = Column(Integer, nullable=True, index=True)

    # Relationships
    creator = relationship("User")

    # We use a string for the relationship to avoid circular import issues with Tag
    tag_as_description = relationship("Tag", back_populates="description_fragment", uselist=False)


class Tag(Base):
    __tablename__ = "tag"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True, nullable=False)

    # A tag can be described by a fragment (making the tag "rich")
    description_fragment_id = Column(Integer, ForeignKey("fragment.id"), nullable=True)

    description_fragment = relationship("Fragment", back_populates="tag_as_description")


class Tagging(Base):
    __tablename__ = "tagging"

    tag_id = Column(Integer, ForeignKey("tag.id"), primary_key=True)
    target_id = Column(Integer, primary_key=True) # Polymorphic ID
    target_type = Column(Integer, primary_key=True) # 1=Tag, 2=Fragment

    tag = relationship("Tag")


class FragmentRelation(Base):
    __tablename__ = "fragment_relation"

    parent_id = Column(Integer, ForeignKey("fragment.id"), primary_key=True)
    child_id = Column(Integer, ForeignKey("fragment.id"), primary_key=True)

    priority = Column(Integer, default=0)
    is_bidirectional = Column(Boolean, default=False)

    parent = relationship("Fragment", foreign_keys=[parent_id])
    child = relationship("Fragment", foreign_keys=[child_id])
