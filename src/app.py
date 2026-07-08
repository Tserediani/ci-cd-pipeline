from fastapi import FastAPI, Path

from typing import Annotated

app = FastAPI()


@app.get("/")
def calculate_sum(x: Annotated[int, Path], y: Annotated[int, Path]) -> int:
    return x + y
