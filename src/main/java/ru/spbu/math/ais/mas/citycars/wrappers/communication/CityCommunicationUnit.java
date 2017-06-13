package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import java.io.Serializable;

public interface CityCommunicationUnit extends Serializable{
	CityMessageSubject getSubject();
}
