import pytest
import random
from src.app import calculate_sum


@pytest.mark.parametrize(
    "x, y", [(random.randint(1, 100), random.randint(1, 100)) for _ in range(5)]
)
def test_calculate_sum(x: int, y: int) -> None:
    assert sum((x, y)) == calculate_sum(x, y)
