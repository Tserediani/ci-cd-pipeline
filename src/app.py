from fastapi import FastAPI, Path

from typing import Annotated

app = FastAPI()


@app.get("/calculate_sum")
def calculate_sum(x: Annotated[int, Path], y: Annotated[int, Path]) -> int:
    return x + y


@app.get("/")
def root() -> dict[str, str]:
    return {"status": "ok"}
