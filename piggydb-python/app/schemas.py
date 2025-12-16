from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class FragmentBase(BaseModel):
    title: Optional[str] = None
    content: Optional[str] = None

class FragmentCreate(FragmentBase):
    pass

class Fragment(FragmentBase):
    id: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    creator_id: Optional[int] = None

    file_name: Optional[str] = None

    class Config:
        from_attributes = True

class TagBase(BaseModel):
    name: str

class Tag(TagBase):
    id: int
    description_fragment_id: Optional[int] = None

    class Config:
        from_attributes = True
