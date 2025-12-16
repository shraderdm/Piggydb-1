from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from . import models, schemas, database

# Create database tables
models.Base.metadata.create_all(bind=database.engine)

app = FastAPI(title="Piggydb API")

# Dependency
def get_db():
    db = database.SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/")
def read_root():
    return {"message": "Welcome to the modernized Piggydb API"}

@app.get("/fragments", response_model=List[schemas.Fragment])
def read_fragments(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    fragments = db.query(models.Fragment).offset(skip).limit(limit).all()
    return fragments

@app.get("/fragments/{fragment_id}", response_model=schemas.Fragment)
def read_fragment(fragment_id: int, db: Session = Depends(get_db)):
    db_fragment = db.query(models.Fragment).filter(models.Fragment.id == fragment_id).first()
    if db_fragment is None:
        raise HTTPException(status_code=404, detail="Fragment not found")
    return db_fragment

@app.post("/fragments", response_model=schemas.Fragment)
def create_fragment(fragment: schemas.FragmentCreate, db: Session = Depends(get_db)):
    # minimal implementation, assuming anonymous/default user for now
    db_fragment = models.Fragment(title=fragment.title, content=fragment.content)
    db.add(db_fragment)
    db.commit()
    db.refresh(db_fragment)
    return db_fragment
